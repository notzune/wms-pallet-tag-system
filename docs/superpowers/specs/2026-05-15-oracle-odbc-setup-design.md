# Oracle ODBC Setup Installer Design

Date: 2026-05-15

## Goal

Build a Windows setup package that prepares a company-managed PC for Oracle 19c 64-bit ODBC connectivity to all Warehouse Analyzer sites. The package is intended for Intune / Company Portal deployment and may run elevated through Intune even when the logged-in user does not have local administrator rights.

The installer must avoid end-user prerequisites, avoid SharePoint or manual download dependencies, and never store or log database credentials. Oracle installer media may be bundled into the generated local executable, but the Oracle ZIP and generated executable must remain local-only artifacts and must not be committed to git.

## Repo And Artifact Layout

Committed source lives under the existing Windows scripting area:

```text
scripts/odbc-setup/
  install.cmd
  install-odbc.ps1
  detect.ps1
  uninstall.ps1
  build-exe.ps1
  README.md
  payload/
    oracle-client-install.rsp.template
```

Generated local artifacts live under ignored output paths:

```text
dist/odbc-setup/
  Tropicana-Oracle-ODBC-Setup.exe
  stage/
    install.cmd
    install-odbc.ps1
    detect.ps1
    uninstall.ps1
    payload/
      O19-64bit.zip
      oracle-client-install.rsp.template
```

`build-exe.ps1` accepts an `-OracleZipPath` argument and copies that ZIP into the staging payload before building the single-file executable. The staged Oracle ZIP is bundled into the executable but is not committed.

## Packaging Approach

The PowerShell installer is the source of truth. The executable is only a wrapper that extracts the same script bundle and runs `install.cmd`.

`build-exe.ps1` should prefer a reliable single-file wrapper:

1. Compile a small .NET Framework self-extracting launcher with the Windows-provided C# compiler when available.
2. Optionally use native Windows self-extraction tooling when it is available and reliable on the build machine.
3. Fail with an actionable message if no supported executable packaging path exists.

The generated executable extracts to a package-owned temporary directory, launches `install.cmd`, preserves logs under `C:\ProgramData\Tropicana\ODBCSetup\Logs`, forwards the installer exit code, and removes temporary extraction files.

## Installer Flow

`install.cmd` launches 64-bit Windows PowerShell with local execution policy bypass and forwards control to `install-odbc.ps1`.

`install-odbc.ps1` performs these steps:

1. Require 64-bit Windows PowerShell.
2. Detect elevation and fail clearly if launched manually without administrator rights.
3. Create `C:\ProgramData\Tropicana\ODBCSetup` and `C:\ProgramData\Tropicana\ODBCSetup\Logs`.
4. Expand bundled `payload\O19-64bit.zip` into a package-owned temporary folder.
5. Generate an Oracle response file from the bundled template with:
   - `ORACLE_BASE=C:\App\Client\Oracle`
   - `ORACLE_HOME=C:\App\Client\Oracle\Product\19.0.0\Client_1`
   - `oracle.install.IsBuiltInAccount=true`
   - `oracle.install.client.installType=Administrator`
6. Run Oracle `setup.exe` silently and capture the exit code and Oracle log location.
7. Treat an already-installed matching Oracle Home as idempotent and repairable rather than fatal.
8. Write `tnsnames.ora` directly to `C:\App\Client\Oracle\Product\19.0.0\Client_1\Network\Admin\tnsnames.ora`.
9. Detect the installed 64-bit Oracle ODBC driver dynamically.
10. Create or repair seven 64-bit System DSNs.
11. Run verification and return a nonzero exit code if required checks fail.

## Site Configuration

The installer writes these aliases to `tnsnames.ora`, all using TCP port `1521` and service name `WMSP`:

| Alias | Site | Host |
| --- | --- | --- |
| TBG1000 | Bradenton | 10.18.228.52 |
| TBG1010 | City of Industry | 10.19.96.103 |
| TBG1011 | Fort Pierce | 10.18.228.72 |
| TBG1279 | Kevita | 10.19.96.104 |
| TBG3002 | Jersey | 10.19.68.61 |
| TBG3230 | Midwest | 10.18.228.66 |
| TBG3322 | Walnut | 10.19.68.53 |

Each entry uses this shape:

```text
TBG1000 =
  (DESCRIPTION =
    (ADDRESS = (PROTOCOL = TCP)(HOST = 10.18.228.52)(PORT = 1521))
    (CONNECT_DATA =
      (SERVICE_NAME = WMSP)
    )
  )
```

## DSN Creation

The installer creates these 64-bit System DSNs:

```text
TBG1000
TBG1010
TBG1011
TBG1279
TBG3002
TBG3230
TBG3322
```

It should prefer PowerShell's built-in ODBC cmdlets when they work for the installed Oracle driver:

```powershell
Add-OdbcDsn -Name TBG1000 -DriverName "Oracle in OraClient19Home1" -DsnType System -Platform "64-bit" -SetPropertyValue @("ServerName=TBG1000")
```

If the cmdlets are unavailable or do not persist the required Oracle values, it should fall back to writing the standard 64-bit ODBC registry keys under:

```text
HKLM\SOFTWARE\ODBC\ODBC.INI
```

The driver detection must reject 32-bit driver names such as `Oracle in OraClient19Home1_32bit`. DSNs must not include `UID`, `PWD`, or any credential-like value.

## Optional Credential Smoke Test

The SOP confirms that credentials are not part of `tnsnames.ora` and are not required to create the System DSNs. Credentials are used when Net Configuration Assistant or ODBC Administrator tests the connection, and the Excel Analyzer connection strings embed `DSN`, `UID`, `PWD`, and `DBQ`.

The installer may support an optional internal smoke test mode that uses the read-only report credentials in memory after DSN creation:

```text
Username: RPTADM
Password: supplied by IT build/deployment configuration
```

This mode should attempt an actual login against each alias or DSN and report pass/fail per site. It must not write `UID` or `PWD` into the System DSNs, `tnsnames.ora`, install state, command lines, or generated response files. Logs may identify the username and test result, for example `TBG3002 credential test passed as RPTADM`, but should not print the password because the exact password value adds no diagnostic value to install logs.

## Detection And Verification

`detect.ps1` and the installer's post-install verification share the same checks:

- Oracle Home path exists.
- 64-bit Oracle ODBC driver is registered.
- `tnsnames.ora` exists and contains all seven aliases.
- All seven 64-bit System DSNs exist.
- Each DSN points to its matching alias or server name.

Verification should continue collecting results after a failure when possible, so Intune logs show a useful report instead of only the first symptom. Optional TCP reachability checks to port `1521` may be supported as non-blocking warnings because VPN, firewall, and network location may vary at install time.

Optional credential smoke tests are verification-only and should be separately controllable from structural detection. `detect.ps1` should continue to verify installed state without requiring live database access or credentials.

## Uninstall Behavior

`uninstall.ps1` removes package-owned DSNs and package-owned metadata by default. It should not remove the Oracle client by default because that client may be shared by other tools.

Oracle client removal may be added behind an explicit switch for IT-controlled scenarios, but it must not be part of the default Company Portal uninstall path.

## Logging And Security

Logs are written to:

```text
C:\ProgramData\Tropicana\ODBCSetup\Logs
```

Install state is written to:

```text
C:\ProgramData\Tropicana\ODBCSetup
```

The installer must never write database passwords to disk, registry, command logs, transcript logs, DSNs, or generated config. Usernames may appear in logs for optional credential smoke test status. Logs should avoid full registry dumps and full future command lines that could include secrets.

## Error Handling

The installer returns nonzero exit codes for these conditions:

- missing payload ZIP
- not elevated
- wrong PowerShell architecture
- Oracle installer failure
- Oracle ODBC driver not found after install
- unable to write Oracle network admin files
- unable to create or repair one or more DSNs
- verification failed after attempted repair

Error messages should name the failed operation, affected path or DSN, and relevant log path.

## Testing

Automated tests should validate pure script behavior without requiring Oracle to be installed:

- generated `tnsnames.ora` includes all seven aliases, hosts, port `1521`, and service `WMSP`
- response-file patching sets Oracle Home, Oracle Base, install type, and built-in account
- DSN cmdlet and registry payload generation omit credentials
- optional credential smoke test logging includes username/test status but omits password
- detection logic reports missing and present components clearly
- packaging refuses to build when the Oracle ZIP path is missing
- generated local artifacts are placed under `dist/odbc-setup`

Manual validation should run on a clean Windows VM through elevated PowerShell or Intune test deployment:

- install from the generated executable
- verify Intune detection succeeds
- verify 64-bit ODBC Administrator shows all seven System DSNs
- verify an Analyzer can connect when credentials are supplied interactively
- rerun installer and confirm idempotent repair with no duplicate DSNs
- run uninstall and confirm DSNs are removed while Oracle client remains installed by default

## Open Decisions

- Confirm the exact executable wrapper implementation after checking build-machine support for `csc.exe` and native self-extraction tooling.
- Confirm whether optional TCP reachability warnings should be enabled by default in logs.
- Confirm whether IT wants an explicit `-RemoveOracleClient` uninstall switch in the first implementation or later.
