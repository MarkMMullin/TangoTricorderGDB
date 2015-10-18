$name = Read-Host 'Prepping to refresh JAVA Side - whozyerdaddy?'
if($name -eq 'C++') {
Write-Host "Removing Java side Java Files"
Remove-Item C:\Dev\tango\as\tricorderJava\app\src\main\java\com\ntx24\Tricorder -Force -Recurse
Write-Host "Copying Java files from VGDB to Java"
Copy-Item C:\Dev\tango\vgdb\Tricorder\Tricorder\src\com\ntx24\tricorder C:\Dev\tango\as\tricorderJava\app\src\main\java\com\ntx24 -recurse
Write-Host "Copying prebuild TangoWrangler library to Java"
Copy-Item C:\Dev\tango\vgdb\Tricorder\Tricorder\libs\armeabi-v7a\libTangoWrangler.so C:\Dev\tango\as\tricorderJava\app\src\main\jniLibs\armeabi-v7a -Force
} else {
    Write-Host "Not a match - expecting transfer from C++ to Java, hence input is C++ - be careful out there"
}