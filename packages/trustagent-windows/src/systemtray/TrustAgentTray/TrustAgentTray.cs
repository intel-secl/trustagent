using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Diagnostics;

namespace TrustAgentTray
{
    public partial class TrustAgentTray : Form

    {

        private NotifyIcon trayIcon;
        private ContextMenu trayMenu;

        public TrustAgentTray()
        {
            // Create a simple tray menu with only one item.
            trayMenu = new ContextMenu();
            trayMenu.MenuItems.Add("Start", OnStart);
            trayMenu.MenuItems.Add("Stop", OnStop);
            trayMenu.MenuItems.Add("About", OnAbout);
            trayMenu.MenuItems.Add("Exit", OnExit);

            // Create a tray icon. In this example we use a
            // standard system icon for simplicity, but you
            // can of course use your own custom icon too.
            trayIcon = new NotifyIcon();
            trayIcon.Text = "TrustAgent";
            //trayIcon.Icon = new Icon(SystemIcons.Application, 40, 40);
            String iconfile = System.Environment.GetEnvironmentVariable("TRUSTAGENT_HOME") + "\\TAicon.ico";
            trayIcon.Icon = new Icon(iconfile);

            // Add menu to tray icon and show it.
            trayIcon.ContextMenu = trayMenu;
            trayIcon.Visible = true;

        }

        protected override void OnLoad(EventArgs e)
        {
            Visible = false; // Hide form window.
            ShowInTaskbar = false; // Remove from taskbar.

            base.OnLoad(e);
        }

        private void OnAbout(object sender, EventArgs e)
        {
            const string message =
                "Intel TrustAgent \n Version 4.0 \n Intel Corporation";
            const string caption = "About TrustAgent";
            var result = MessageBox.Show(message, caption,
                                             MessageBoxButtons.OK,
                                             MessageBoxIcon.Information);
        }

        private void OnSetup(object sender, EventArgs e)
        {
            Process myProcess = new Process();
            String setup_path = Environment.GetEnvironmentVariable("TRUSTAGENT_HOME") + "\\bin\\tasetup.cmd";

            try
            {
                OnStop(sender, e);
                myProcess.StartInfo.UseShellExecute = false;
                myProcess.StartInfo.FileName = setup_path;
                //myProcess.StartInfo.CreateNoWindow = true;
                myProcess.Start();
            }
            catch (Exception ee)
            {
                Console.WriteLine(ee.Message);
            }
        }

        private void OnExit(object sender, EventArgs e)
        {
            Application.Exit();
        }

        private void OnStart(object sender, EventArgs e)
        {
            Process myProcess = new Process();

            try
            {
                myProcess.StartInfo.UseShellExecute = false;
                myProcess.StartInfo.FileName = "net";
                myProcess.StartInfo.Arguments = "start TrustAgent";
                myProcess.StartInfo.CreateNoWindow = true;
                myProcess.Start();
            }
            catch (Exception ee)
            {
                Console.WriteLine(ee.Message);
            }
        }

        private void OnStop(object sender, EventArgs e)
        {
            Process myProcess = new Process();
            //Process myProcess1 = new Process();
            string strCmdText;

            try
            {
                myProcess.StartInfo.UseShellExecute = false;
                myProcess.StartInfo.FileName = "net";
                myProcess.StartInfo.Arguments = "stop TrustAgent";
                myProcess.StartInfo.CreateNoWindow = true;
                myProcess.Start();

                //myProcess1.StartInfo.WindowStyle = System.Diagnostics.ProcessWindowStyle.Hidden;
                strCmdText= "/C wmic process where \"CommandLine like '%%mtwilson%%' and name like '%%java%%'\" call terminate";
                System.Diagnostics.Process.Start("CMD.exe", strCmdText);
            }
            catch (Exception ee)
            {
                Console.WriteLine(ee.Message);
            }
        }

    }
}