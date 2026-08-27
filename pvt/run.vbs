Option Explicit

Dim WshShell, fso, strPath, jarPath, command
Dim wmi, processes, processItem, processCommand
Dim mysqlExePath, mysqlIniPath, mysqlCommand
Dim mysqlPort, mysqlWaitSeconds
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Side-effect-free mode used to validate VBScript syntax before deployment.
If WScript.Arguments.Named.Exists("check") Then
    WScript.Echo "Run web PVT launcher syntax OK."
    WScript.Quit 0
End If

strPath = fso.GetParentFolderName(WScript.ScriptFullName)
jarPath = fso.BuildPath(strPath, "pvt.jar")

' Verified MySQL installation on the OTHPDN Windows server.
mysqlExePath = "E:\laragon\bin\mysql\mysql-8.4.3-winx64\bin\mysqld.exe"
mysqlIniPath = "E:\laragon\bin\mysql\mysql-8.4.3-winx64\my.ini"
mysqlPort = 3306
mysqlWaitSeconds = 60

If Not fso.FileExists(jarPath) Then
    MsgBox "pvt.jar was not found in:" & vbCrLf & strPath, _
        vbCritical, "Run web PVT"
    WScript.Quit 1
End If

' Set the deployment directory before MySQL and Java are launched.
WshShell.CurrentDirectory = strPath

' Start MySQL only when port 3306 is unavailable. The bounded readiness
' check is safer than a fixed delay because InnoDB crash recovery can vary.
If Not IsPortListening(mysqlPort) Then
    If Not StartMySqlAndWait() Then
        WScript.Quit 1
    End If
End If

' Do not start a second process for this exact pvt.jar.
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

' Start the application in the background with UTF-8 encoding.
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

Function StartMySqlAndWait()
    Dim waitedSeconds

    StartMySqlAndWait = False

    If Not fso.FileExists(mysqlExePath) Then
        MsgBox "MySQL could not be found:" & vbCrLf & mysqlExePath & vbCrLf & vbCrLf & _
            "Run web PVT did not start the JAR.", _
            vbCritical, "Run web PVT"
        Exit Function
    End If

    If Not fso.FileExists(mysqlIniPath) Then
        MsgBox "MySQL configuration could not be found:" & vbCrLf & mysqlIniPath & vbCrLf & vbCrLf & _
            "Run web PVT did not start the JAR.", _
            vbCritical, "Run web PVT"
        Exit Function
    End If

    ' A second click may arrive while the first MySQL process is starting.
    If Not IsProcessRunning("mysqld.exe") Then
        mysqlCommand = """" & mysqlExePath & """ --defaults-file=""" & _
            mysqlIniPath & """ --console"

        On Error Resume Next
        WshShell.Run mysqlCommand, 0, False
        If Err.Number <> 0 Then
            MsgBox "MySQL could not be started." & vbCrLf & vbCrLf & _
                "Details: " & Err.Description, vbCritical, "Run web PVT"
            Err.Clear
            On Error GoTo 0
            Exit Function
        End If
        On Error GoTo 0
    End If

    waitedSeconds = 0
    Do While waitedSeconds < mysqlWaitSeconds
        WScript.Sleep 1000
        waitedSeconds = waitedSeconds + 1

        If IsPortListening(mysqlPort) Then
            StartMySqlAndWait = True
            Exit Function
        End If
    Loop

    MsgBox "MySQL did not become ready on port " & mysqlPort & _
        " within " & mysqlWaitSeconds & " seconds." & vbCrLf & vbCrLf & _
        "The PVT JAR was not started. Check the MySQL error log in Laragon.", _
        vbCritical, "Run web PVT"
End Function

Function IsPortListening(portNumber)
    Dim powershellPath, powershellCode, checkCommand, exitCode

    powershellPath = WshShell.ExpandEnvironmentStrings( _
        "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe")
    powershellCode = "$client = New-Object System.Net.Sockets.TcpClient; " & _
        "try { $client.Connect('127.0.0.1'," & CStr(portNumber) & "); exit 0 } " & _
        "catch { exit 1 } finally { if ($null -ne $client) { $client.Dispose() } }"
    checkCommand = """" & powershellPath & _
        """ -NoProfile -NonInteractive -Command """ & powershellCode & """"

    On Error Resume Next
    exitCode = WshShell.Run(checkCommand, 0, True)
    If Err.Number <> 0 Then
        Err.Clear
        IsPortListening = False
    Else
        IsPortListening = (exitCode = 0)
    End If
    On Error GoTo 0
End Function

Function IsProcessRunning(processName)
    Dim localWmi, localProcesses, localProcess

    IsProcessRunning = False

    On Error Resume Next
    Set localWmi = GetObject("winmgmts:\\.\root\cimv2")
    Set localProcesses = localWmi.ExecQuery( _
        "SELECT ProcessId FROM Win32_Process WHERE Name='" & processName & "'")
    If Err.Number = 0 Then
        For Each localProcess In localProcesses
            IsProcessRunning = True
            Exit For
        Next
    End If
    Err.Clear
    On Error GoTo 0
End Function
