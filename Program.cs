using System;
using System.Threading.Tasks;

namespace GrooveCoasterController
{
    class Program
    {
        static async Task Main(string[] args)
        {
            Console.WriteLine("Starting...");
            var b2k = new Bluetooth2Key();
            await b2k.IntervalPress();
        }
    }
}