Option Explicit

Dim WshShell, fso, strPath, jarPath, command
Dim wmi, processes, processItem, processCommand
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

strPath = fso.GetParentFolderName(WScript.ScriptFullName)
jarPath = fso.BuildPath(strPath, "pvt.jar")

If Not fso.FileExists(jarPath) Then
    MsgBox "pvt.jar was not found in:" & vbCrLf & strPath, _
        vbCritical, "Run web PVT"
    WScript.Quit 1
End If

' Không khởi động tiến trình thứ hai nếu đúng pvt.jar này đang chạy.
' Việc này tránh lỗi port 80 và tránh trạng thái trộn class khi cập nhật JAR.
On Error Resume Next
Set wmi = GetObject("winmgmts:\\.\root\cimv2")
Set processes = wmi.ExecQuery( _
    "SELECT CommandLine FROM Win32_Process " & _
    "WHERE Name='javaw.exe' OR Name='java.exe'")
If Err.Number = 0 Then
    For Each processItem In processes
        If Not IsNull(processItem.CommandLine) Then
            processCommand = CStr(processItem.CommandLine)
            If InStr(1, processCommand, jarPath, vbTextCompare) > 0 Then
                MsgBox "PVT web server is already running.", _
                    vbInformation, "Run web PVT"
                WScript.Quit 0
            End If
        End If
    Next
End If
Err.Clear
On Error GoTo 0

' Đảm bảo thư mục logs được tạo cạnh pvt.jar.
WshShell.CurrentDirectory = strPath

' Chạy ứng dụng nền với encoding UTF-8.
command = "javaw -Dfile.encoding=UTF-8 -jar """ & jarPath & """"

On Error Resume Next
WshShell.Run command, 0, False
If Err.Number <> 0 Then
    MsgBox "PVT web server could not be started." & vbCrLf & _
        "Check that Java is installed and javaw is available in PATH." & vbCrLf & vbCrLf & _
        "Details: " & Err.Description, vbCritical, "Run web PVT"
    WScript.Quit 1
End If
On Error GoTo 0
