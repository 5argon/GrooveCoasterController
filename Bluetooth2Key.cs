using System;
using System.Threading.Tasks;
using WindowsInput;
using WindowsInput.Native;
using InTheHand.Net.Sockets;
using System.IO;

namespace GrooveCoasterController
{
    class Bluetooth2Key
    {
        private InputSimulator IS { get; }
        private BluetoothListener Listener { get; }
        readonly Guid OurServiceClassId = new Guid("{0abfa6c2-384c-4844-8e9d-7fcc862b3a7d}");

        public Bluetooth2Key()
        {
            IS = new InputSimulator();
            try
            {
                Listener = new BluetoothListener(OurServiceClassId);
            }
            catch (System.PlatformNotSupportedException)
            {
                Console.WriteLine($"Ouch! Did you turn on the Bluetooth?");
                throw;
            }
            StartBluetoothRFCOMM();
        }

        private void StartBluetoothRFCOMM()
        {
            Listener.ServiceName = "Groove Coaster Controller";
            Listener.Start();
            Task.Run(() => ListenerAsync(Listener));
            Console.WriteLine("Starting Bluetooth listener!");
        }

        private Task ListenerAsync(BluetoothListener listener)
        {
            while (true)
            {
                var conn = listener.AcceptBluetoothClient();
                var peer = conn.GetStream();
                Console.WriteLine($"Connected from : {conn.RemoteEndPoint.Address} {conn.RemoteMachineName}");
                ReadMessagesToEnd(peer);
            }
        }

        private void ReadMessagesToEnd(Stream peer)
        {
            var rdr = new StreamReader(peer);
            while (true)
            {
                string line;
                try
                {
                    line = rdr.ReadLine();
                }
                catch (IOException ioex)
                {
                    Console.WriteLine("Connection closed hard! (read)");
                    Console.WriteLine(ioex);
                    break;
                }
                if (line == null)
                {
                    Console.WriteLine("Connection closed (read)");
                    break;
                }
                Console.WriteLine($"Message : {line}");
            }
            ConnectionCleanup();
        }

        private void ConnectionCleanup()
        {
            Console.WriteLine($"Cleaning up connection");
        }

        static int counter = 0;
        public async Task IntervalPress()
        {
            while (true)
            {
                IS.Keyboard.KeyDown(VirtualKeyCode.VK_A);
                IS.Keyboard.KeyUp(VirtualKeyCode.VK_A);
                Console.WriteLine("Pressed! " + ++counter);
                await Task.Delay(500);
            }
        }

        public async Task WaitForever()
        {
            await Task.Delay(-1);
        }
    }
}