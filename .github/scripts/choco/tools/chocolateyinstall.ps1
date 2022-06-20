$ErrorActionPreference = 'Stop';
$url64      = '@LAUNCHER_URL@'
$packageArgs = @{
  packageName   = 'scala-cli'
  fileType      = 'MSI'
  url64bit      = $url64

  softwareName  = 'Scala CLI' 
  checksum64    = '@LAUNCHER_SHA256@'
  checksumType64= 'sha256'

  silentArgs    = "/qn /norestart"
  validExitCodes= @(0)
}

Install-ChocolateyPackage @packageArgs