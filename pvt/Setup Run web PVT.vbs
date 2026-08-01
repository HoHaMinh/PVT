Option Explicit

Dim WshShell, fso, strPath, launcherPath, iconPath
Dim shortcutPath, desktopPath, shortcut, answer

Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

strPath = fso.GetParentFolderName(WScript.ScriptFullName)
launcherPath = fso.BuildPath(strPath, "run.vbs")
iconPath = fso.BuildPath(strPath, "pvt-launcher.ico")
shortcutPath = fso.BuildPath(strPath, "Run web PVT.lnk")

If Not fso.FileExists(launcherPath) Then
    MsgBox "run.vbs was not found in:" & vbCrLf & strPath, _
        vbCritical, "Setup Run web PVT"
    WScript.Quit 1
End If

If Not fso.FileExists(iconPath) Then
    MsgBox "pvt-launcher.ico was not found in:" & vbCrLf & strPath, _
        vbCritical, "Setup Run web PVT"
    WScript.Quit 1
End If

CreatePvtShortcut shortcutPath

answer = MsgBox( _
    "The 'Run web PVT' shortcut was created beside pvt.jar." & vbCrLf & vbCrLf & _
    "Would you like to copy this shortcut to the Desktop?", _
    vbQuestion + vbYesNo, "Setup Run web PVT")

If answer = vbYes Then
    desktopPath = fso.BuildPath(WshShell.SpecialFolders("Desktop"), "Run web PVT.lnk")
    ' Sao chép nguyên shortcut đã kiểm tra thay vì tạo một shortcut thứ hai.
    ' Cách này tương đương thao tác Send to > Desktop của Windows.
    fso.CopyFile shortcutPath, desktopPath, True
    MsgBox "The Desktop shortcut was created successfully.", _
        vbInformation, "Setup Run web PVT"
End If

Sub CreatePvtShortcut(targetShortcutPath)
    Set shortcut = WshShell.CreateShortcut(targetShortcutPath)
    shortcut.TargetPath = WshShell.ExpandEnvironmentStrings("%WINDIR%\System32\wscript.exe")
    shortcut.Arguments = """" & launcherPath & """"
    shortcut.WorkingDirectory = strPath
    shortcut.IconLocation = iconPath & ",0"
    shortcut.Description = "Start PVT web server"
    shortcut.WindowStyle = 7
    shortcut.Save
End Sub
