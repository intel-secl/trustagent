# Instructions to Build Hardened NSIS Installer on Windows

## Prerequisites:
a) Microsoft Visual Studio Professional 2017  
b) HTML Help Workshop
1.  Download from  [https://www.microsoft.com/en-us/download/details.aspx?id=21138](https://www.microsoft.com/en-us/download/details.aspx?id=21138)
2.  Add  _hhc.exe_  to 'PATH' Environment Variable.

c) Python 2.7
1.  Download from  [https://www.python.org/downloads/windows/](https://www.python.org/downloads/windows/)

d) SCons
1.  '_pip install scons_' may fail.
2.  In such a case, download SCons from  [http://prdownloads.sourceforge.net/scons/scons-3.0.5.zip](http://prdownloads.sourceforge.net/scons/scons-3.0.5.zip)
3.  Extract the source and execute '_python setup.py install_' from extracted directory.

e) Zlib
1.  Download zlib-1.2.7 for windows from  [https://nsis.sourceforge.io/mediawiki/images/c/ca/Zlib-1.2.7-win32-x86.zip](https://nsis.sourceforge.io/mediawiki/images/c/ca/Zlib-1.2.7-win32-x86.zip)
2.  Set the extracted location to ZLIB_W32 environment variable.

## Build:

 1. Checkout customized NSIS from git repo [https://gitlab.devtools.intel.com/sst/isecl/trustagent.git](https://gitlab.devtools.intel.com/sst/isecl/trustagent.git)
 2. Run '_scons dist-installer_' under '_packages/nsis-installer'_ directory to build. 
 3. A self-extracting NSIS installer could be found under the root directory named  _nsis-{date}.cvs-setup.exe_ 
 4. Install it prior to building TrustAgent for Windows.
