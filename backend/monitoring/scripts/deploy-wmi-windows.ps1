# ============================================================
# DÉPLOIEMENT WMI EXPORTER — SERVEURS WINDOWS
# ============================================================

param(
    [string[]]$Servers = @(""),
    [string]$Username = "administrateur",
    [string]$Password = ""
)

# Demander le mot de passe si non fourni
if ([string]::IsNullOrEmpty($Password)) {
    $SecurePassword = Read-Host "Mot de passe pour $Username" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePassword))
}

$Credential = New-Object PSCredential($Username, 
    (ConvertTo-SecureString $Password -AsPlainText -Force))

$WmiUrl = "https://github.com/prometheus-community/windows_exporter/releases/download/v0.25.0/windows_exporter-0.25.0-amd64.msi"
$LocalMsi = "$env:TEMP\windows_exporter.msi"

Write-Host "Téléchargement du MSI..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $WmiUrl -OutFile $LocalMsi -UseBasicParsing

foreach ($Server in $Servers) {
    Write-Host "`n--- Traitement de $Server ---" -ForegroundColor Yellow
    
    try {
        # Copier le MSI
        Copy-Item -Path $LocalMsi -Destination "\\$Server\C$\Temp\windows_exporter.msi" -Force
        
        # Installer à distance
        Invoke-Command -ComputerName $Server -Credential $Credential -ScriptBlock {
            msiexec /i "C:\Temp\windows_exporter.msi" ENABLED_COLLECTORS=cpu,memory,logical_disk,net,os,system /quiet /norestart
            Start-Service windows_exporter -ErrorAction SilentlyContinue
        }
        
        # Vérifier
        $Test = Invoke-Command -ComputerName $Server -Credential $Credential -ScriptBlock {
            try { (Invoke-WebRequest "http://localhost:9182/metrics" -UseBasicParsing).StatusCode } 
            catch { 0 }
        }
        
        if ($Test -eq 200) {
            Write-Host "✅ WMI Exporter OK sur $Server" -ForegroundColor Green
        } else {
            Write-Host "⚠️ WMI Exporter installé mais ne répond pas sur $Server" -ForegroundColor Yellow
        }
        
    } catch {
        Write-Host "❌ Erreur sur $Server : $_" -ForegroundColor Red
    }
}

Write-Host "`nDéploiement terminé." -ForegroundColor Cyan