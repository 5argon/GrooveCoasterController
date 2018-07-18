using System;
using System.Threading.Tasks;
using System.Diagnostics;

namespace GrooveCoasterController
{
    class Program
    {
        static async Task Main(string[] args)
        {
            Console.WriteLine("=== Groove Coaster Controller (Android) Server ===");
            Console.WriteLine("Project page : http://5argon.info/gccon");
            Console.WriteLine("GitHub page : https://github.com/5argon/GrooveCoasterController");
            Console.WriteLine("Contact : @5argondesu / pyasry@gmail.com / GitHub page's issue section");
            Console.WriteLine("==================================================");
            var b2k = new Bluetooth2Key();
            await b2k.StartBluetoothRFCOMM();
        }
    }
}