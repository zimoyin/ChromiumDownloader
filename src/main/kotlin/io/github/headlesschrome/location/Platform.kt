package io.github.headlesschrome.location

import java.util.*

/**
 *
 * @author : zimo
 * @date : 2025/02/08
 */
enum class Platform {
    Android,
    AndroidDesktop_arm64,
    AndroidDesktop_x64,
    Android_Arm64,
    Arm,
    Linux,
    LinuxGit,
    LinuxGit_x64,
    Linux_ARM_Cross_Compile,
    Linux_ChromiumOS,
    Linux_ChromiumOS_Full,
    Linux_x64,
    Mac,
    MacGit,
    Mac_Arm,
    Win,
    WinGit,
    Win_Arm64,
    Win_x64,
    android_rel,
    chromium_full_linux_chromeos,
    experimental,
    gs_test,
    icons,
    lacros64,
    lacros_arm,
    lacros_arm64,
    linux_lacros,
    linux_rel,
    mac_rel,
    tmp,
    win32_rel,
    win_rel,
    Unknown,
    ;

    companion object {
        val OS = System.getProperty("os.name").lowercase(Locale.getDefault())
        fun currentPlatform(): Platform {
            if (OS.contains("win")) return Win
            if (OS.contains("mac")) return Mac
            if (OS.contains("linux")) return Linux_x64
            return Unknown
        }
    }
}