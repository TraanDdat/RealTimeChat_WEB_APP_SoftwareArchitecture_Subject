$env:JAVA_HOME="C:\toolbase\java_jdk\21.0.7.6"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

Write-Host "JAVA_HOME = $env:JAVA_HOME"
java -version
javac -version