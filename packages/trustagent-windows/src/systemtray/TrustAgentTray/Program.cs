using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace TrustAgentTray
{
    static class Program
    {
        //TODO :: Validate : The Whitelist of DLLs for TrustAgent Service
        //TODO :: Whitelist in Non-String Form
        //TODO :: Validate : AppInit_DLLs, AppCertDLLs, KnownDlls

        /* The whitelist of DLLs that we load dynamically */
        static string[] wl = new string[] { "advapi32.dll" };
        static HashSet<string> dllwl = new HashSet<string>(wl);

        static LogWriter lw = new LogWriter("Trust Agent Tray Logger");

        //The Foundation of DLL Injection Monitoring
        [DllImport("kernel32", CharSet = CharSet.Ansi, ExactSpelling = true, SetLastError = true)]
        static extern IntPtr GetProcAddress(IntPtr hModule, string procName);

        [DllImport("kernel32", SetLastError = true, CharSet = CharSet.Ansi)]
        static extern IntPtr LoadLibrary([MarshalAs(UnmanagedType.LPStr)]string lpFileName);

        delegate IntPtr LoadLibraryW(string lpFileName);
        delegate IntPtr GetModuleHandleAW(string lpModuleName);
        delegate long LdrLoadDllW(string lpFileName,
                                  ulong[] lFlags,
                                  string wParam,
                                  IntPtr hModule);

        static WHook hookLLA, hookLLD, hookGMH;

        static long LdrLoadDll_Hook(string lpFileName,
                                    ulong[] lFlags,
                                    string wParam,
                                    IntPtr hModule)
        {
            lw.LogWrite("LdrLoadDll Blocking DLL Loading " + lpFileName);
            return 0;
        }

        static IntPtr GetModuleHandleA_Hook(string lpModuleName)
        {
            lw.LogWrite("Blocking Load of " + lpModuleName + " DLL");
            //Return Results in LoaderLock Exception, Which Cant Be Handled in This Scope
            //return IntPtr.Zero;
            throw new Exception();
        }

        static IntPtr LoadLibrary_Hook(string lpFileName)
        {
            IntPtr result;
            hookLLA.Uninstall();
            if (!dllwl.Contains(lpFileName))
            {
                hookLLA.Install();
                lw.LogWrite("DLL Check for " + lpFileName + " Failed");
                result = IntPtr.Zero;
                return result;
            }
            //lw.LogWrite("Loading DLL " + lpFileName);
            result = LoadLibrary(lpFileName);
            hookLLA.Install();
            return result;
        }
        static void TrustAgentUEEHandler(object sender, UnhandledExceptionEventArgs e)
        {
            lw.LogWrite((e.ExceptionObject as Exception).Message);
            Exception ex = (Exception)e.ExceptionObject;
            Environment.Exit(ex.HResult);
        }

        [STAThread]
        static void Main()
        {
            AppDomain.CurrentDomain.UnhandledException += new UnhandledExceptionEventHandler(TrustAgentUEEHandler);

            var k32 = LoadLibrary("kernel32");
            var ntd = LoadLibrary("ntdll");

            var lla = GetProcAddress(k32, "LoadLibraryA");
            var gmh = GetProcAddress(k32, "GetModuleHandleA");
            //var lld = GetProcAddress(ntd, "LdrLoadDll");

            hookLLA = new WHook(lla, (LoadLibraryW)LoadLibrary_Hook);
            hookGMH = new WHook(gmh, (GetModuleHandleAW)GetModuleHandleA_Hook);
            //hookLLD = new WHook(lld, (LdrLoadDllW)LdrLoadDll_Hook);

            hookLLA.Install();
            hookGMH.Install();
            //hookLLD.Install();

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new TrustAgentTray());
        }
    }
}
