package com.asuka.player.app

internal const val ROUTE_HOME = "home"
internal const val ROUTE_ALL_VIDEOS = "all_videos"
internal const val ROUTE_RECENT = "recent"
internal const val ROUTE_SETTINGS = "settings"
internal const val ROUTE_SETTINGS_PLAYER = "settings/player"
internal const val ROUTE_SETTINGS_THEME = "settings/theme"
internal const val ROUTE_SETTINGS_MOTION = "settings/motion"
internal const val ARG_FOLDER_ID = "folderId"
internal const val ROUTE_FOLDER = "folder/{$ARG_FOLDER_ID}"

internal fun folderRoute(folderId: Long): String = "folder/$folderId"
