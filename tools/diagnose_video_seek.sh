#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 /absolute/or/relative/path/to/video.mp4"
  exit 1
fi

if ! command -v ffprobe >/dev/null 2>&1; then
  echo "Error: ffprobe not found. Install ffmpeg first."
  echo "macOS: brew install ffmpeg"
  exit 2
fi

video_path="$1"
if [[ ! -f "$video_path" ]]; then
  echo "Error: file not found: $video_path"
  exit 3
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

format_file="$tmp_dir/format.txt"
video_stream_file="$tmp_dir/video_stream.txt"
keyframe_file="$tmp_dir/keyframes.txt"

ffprobe -v error \
  -show_entries format=format_name,duration,start_time,bit_rate,size \
  -of default=noprint_wrappers=1:nokey=0 \
  "$video_path" >"$format_file"

ffprobe -v error -select_streams v:0 \
  -show_entries stream=index,codec_name,profile,pix_fmt,width,height,avg_frame_rate,r_frame_rate,nb_frames,duration,bit_rate \
  -of default=noprint_wrappers=1:nokey=0 \
  "$video_path" >"$video_stream_file"

ffprobe -v error -select_streams v:0 \
  -show_frames \
  -show_entries frame=key_frame,best_effort_timestamp_time,pkt_dts_time,pkt_pts_time \
  -of csv=p=0 \
  "$video_path" >"$tmp_dir/frames.csv"

awk -F',' '
  {
    key=$1
    t=$2
    if (t == "" || t == "N/A") t=$3
    if (t == "" || t == "N/A") t=$4
    if (key == 1 && t != "" && t != "N/A") print t
  }
' "$tmp_dir/frames.csv" >"$keyframe_file"

frame_stats_file="$tmp_dir/frame_stats.txt"
awk -F',' '
  BEGIN {
    prev = ""
    regress = 0
    gaps_gt_1s = 0
    count = 0
  }
  {
    t=$2
    if (t == "" || t == "N/A") t=$3
    if (t == "" || t == "N/A") t=$4
    if (t == "" || t == "N/A") next
    ts=t+0.0
    count++
    if (prev != "" && ts < prev) regress++
    if (prev != "" && (ts - prev) > 1.0) gaps_gt_1s++
    prev=ts
  }
  END {
    printf("frame_count=%d\n", count)
    printf("timestamp_regressions=%d\n", regress)
    printf("large_timestamp_gaps_gt_1s=%d\n", gaps_gt_1s)
  }
' "$tmp_dir/frames.csv" >"$frame_stats_file"

key_stats_file="$tmp_dir/key_stats.txt"
awk '
  BEGIN {
    prev = ""
    count = 0
    min = -1
    max = 0
    sum = 0
  }
  {
    t=$1+0.0
    count++
    if (prev != "") {
      d=t-prev
      sum+=d
      if (min < 0 || d < min) min=d
      if (d > max) max=d
    }
    prev=t
  }
  END {
    intervals=(count > 1 ? count - 1 : 0)
    avg=(intervals > 0 ? sum / intervals : 0)
    printf("keyframe_count=%d\n", count)
    printf("keyframe_intervals=%d\n", intervals)
    printf("keyframe_interval_min_sec=%.3f\n", min < 0 ? 0 : min)
    printf("keyframe_interval_avg_sec=%.3f\n", avg)
    printf("keyframe_interval_max_sec=%.3f\n", max)
  }
' "$keyframe_file" >"$key_stats_file"

echo "=== Video Seek Diagnostic ==="
echo "file=$video_path"
echo
cat "$format_file"
echo
cat "$video_stream_file"
echo
cat "$frame_stats_file"
cat "$key_stats_file"
echo

duration="$(awk -F= '/^duration=/{print $2; exit}' "$format_file")"
key_count="$(awk -F= '/^keyframe_count=/{print $2; exit}' "$key_stats_file")"
key_max="$(awk -F= '/^keyframe_interval_max_sec=/{print $2; exit}' "$key_stats_file")"
regressions="$(awk -F= '/^timestamp_regressions=/{print $2; exit}' "$frame_stats_file")"

duration_ok=1
if [[ -z "${duration:-}" || "$duration" == "N/A" ]]; then
  duration_ok=0
else
  # bc is not guaranteed, use awk numeric check
  if ! awk -v d="$duration" 'BEGIN{exit !(d+0 > 0)}'; then
    duration_ok=0
  fi
fi

verdict="GOOD"
reasons=()

if [[ "$duration_ok" -eq 0 ]]; then
  verdict="RISKY"
  reasons+=("duration missing_or_non_positive")
fi

if awk -v k="$key_count" 'BEGIN{exit !(k+0 < 2)}'; then
  verdict="RISKY"
  reasons+=("too_few_keyframes")
fi

if awk -v m="$key_max" 'BEGIN{exit !(m+0 > 10)}'; then
  verdict="RISKY"
  reasons+=("keyframe_interval_too_large_gt_10s")
fi

if awk -v r="$regressions" 'BEGIN{exit !(r+0 > 0)}'; then
  verdict="RISKY"
  reasons+=("timestamp_regression_detected")
fi

echo "verdict=$verdict"
if [[ ${#reasons[@]} -eq 0 ]]; then
  echo "reason=seek_should_be_stable"
else
  echo "reason=${reasons[*]}"
fi

echo
echo "Tips:"
echo "- If verdict=RISKY, remux test: ffmpeg -i input.mp4 -c copy -movflags +faststart output.mp4"
echo "- If still bad, re-encode with denser keyframes (e.g. -g 48)."
