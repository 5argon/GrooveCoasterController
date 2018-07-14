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
        readonly Guid OurServiceClassId = new Guid("0abfa6c2-384c-4844-8e9d-7fcc862b3a7d");

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
            Console.WriteLine("Starting Bluetooth listener!");
            Task.Run(() => ListenerAsync(Listener));
        }

        private void ListenerAsync(BluetoothListener listener)
        {
            Console.WriteLine($"Waiting for connection...");
            BluetoothClient client = listener.AcceptBluetoothClient();
            Console.WriteLine($"Connected from : {client.RemoteEndPoint.Address} {client.RemoteMachineName}");
            var peer = client.GetStream();
            Console.WriteLine("Reading...");
            while(true)
            {
                ReadMessagesToEnd(peer);
            }
        }

        private void ReadMessagesToEnd(Stream peer)
        {
            var rdr = new StreamReader(peer);
            while (true)
            {
                int r;
                try
                {
                    r = rdr.Read(); //Blocking call
                    IS.Keyboard.KeyDown(VirtualKeyCode.VK_A);
                    IS.Keyboard.KeyUp(VirtualKeyCode.VK_A);
                }
                catch (IOException ioex)
                {
                    Console.WriteLine("Connection closed hard! (read)");
                    Console.WriteLine(ioex);
                    break;
                }
                Console.WriteLine($"Message : {r}");
            }
            ConnectionCleanup();
        }

        private void ConnectionCleanup()
        {
            Console.WriteLine($"Cleaning up connection");
        }

        public async Task WaitForever()
        {
            await Task.Delay(-1);
        }
    }
}