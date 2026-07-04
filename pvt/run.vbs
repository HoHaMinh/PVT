Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
strPath = fso.GetParentFolderName(WScript.ScriptFullName)

' Đảm bảo thư mục logs được tạo cạnh pvt.jar.
WshShell.CurrentDirectory = strPath

' Chạy ứng dụng nền với encoding UTF-8.
WshShell.Run "javaw -Dfile.encoding=UTF-8 -jar """ & strPath & "\pvt.jar""", 0, False
