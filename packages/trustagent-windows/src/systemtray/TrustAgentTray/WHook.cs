using System;
using System.Runtime.InteropServices;

namespace TrustAgentTray
{
    public class WHook : IDisposable
    {

        const int nBytes = 5;

        IntPtr addr;
        Protection old;
        byte[] src = new byte[5];
        byte[] dst = new byte[5];

        public WHook(IntPtr source, IntPtr destination)
        {
            VirtualProtect(source, nBytes, Protection.PAGE_EXECUTE_READWRITE, out old);
            Marshal.Copy(source, src, 0, nBytes);
            dst[0] = 0xE9;
            var dx = BitConverter.GetBytes((int)destination - (int)source - nBytes);
            Array.Copy(dx, 0, dst, 1, nBytes - 1);
            addr = source;
        }
        public WHook(IntPtr source, Delegate destination) :
            this(source, Marshal.GetFunctionPointerForDelegate(destination))
        {
        }

        public void Install()
        {
            Marshal.Copy(dst, 0, addr, nBytes);
        }

        public void Uninstall()
        {
            Marshal.Copy(src, 0, addr, nBytes);
        }

        public void Dispose()
        {
            Uninstall();
            Protection x;
            VirtualProtect(addr, nBytes, old, out x);
        }

        [DllImport("kernel32.dll", SetLastError = true)]
        static extern bool VirtualProtect(IntPtr lpAddress, uint dwSize,
            Protection flNewProtect, out Protection lpflOldProtect);

        public enum Protection
        {
            PAGE_NOACCESS = 0x01,
            PAGE_READONLY = 0x02,
            PAGE_READWRITE = 0x04,
            PAGE_WRITECOPY = 0x08,
            PAGE_EXECUTE = 0x10,
            PAGE_EXECUTE_READ = 0x20,
            PAGE_EXECUTE_READWRITE = 0x40,
            PAGE_EXECUTE_WRITECOPY = 0x80,
            PAGE_GUARD = 0x100,
            PAGE_NOCACHE = 0x200,
            PAGE_WRITECOMBINE = 0x400
        }

    }
}


