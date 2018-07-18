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
        private InputSimulator InputSimulator { get; }
        private BluetoothListener Listener { get; }
        readonly Guid OurServiceClassId = new Guid("0abfa6c2-384c-4844-8e9d-7fcc862b3a7d");

        public Bluetooth2Key()
        {
            InputSimulator = new InputSimulator();
            try
            {
                Listener = new BluetoothListener(OurServiceClassId);
            }
            catch (System.PlatformNotSupportedException)
            {
                Console.WriteLine($"Ouch! Did you turn on the Bluetooth?");
                throw;
            }
        }

        public async Task StartBluetoothRFCOMM()
        {
            Listener.ServiceName = "Groove Coaster Controller Server";
            Listener.Start();
            Console.WriteLine("Starting Bluetooth listener!");
            while(true)
            {
                await Task.Run(() => ListenerAsync(Listener));
            }
        }

        private void ListenerAsync(BluetoothListener listener)
        {
            Console.WriteLine($"Waiting for connection...");
            BluetoothClient client = listener.AcceptBluetoothClient();
            Console.WriteLine($"Connected from : {client.RemoteMachineName} (Address : {client.RemoteEndPoint.Address})");
            var peer = client.GetStream();
            Console.WriteLine("Reading input...");
            ReadMessagesToEnd(peer);
        }

        private abstract class SideConfig
        {
            public abstract VirtualKeyCode left { get; }
            public abstract VirtualKeyCode down { get; }
            public abstract VirtualKeyCode up { get; }
            public abstract VirtualKeyCode right { get; }
            public abstract VirtualKeyCode press { get; }
            public abstract VirtualKeyCode escape { get; }
            public abstract VirtualKeyCode back { get; }
        }

        private class LeftConfig : SideConfig
        {
            public override VirtualKeyCode left => VirtualKeyCode.VK_A;
            public override VirtualKeyCode down => VirtualKeyCode.VK_S;
            public override VirtualKeyCode up => VirtualKeyCode.VK_W;
            public override VirtualKeyCode right => VirtualKeyCode.VK_D;
            public override VirtualKeyCode press => VirtualKeyCode.SPACE;
            public override VirtualKeyCode escape => VirtualKeyCode.ESCAPE;
            public override VirtualKeyCode back => VirtualKeyCode.DELETE;
        }

        private class RightConfig : SideConfig
        {
            public override VirtualKeyCode left => VirtualKeyCode.NUMPAD4;
            public override VirtualKeyCode down => VirtualKeyCode.NUMPAD5;
            public override VirtualKeyCode up => VirtualKeyCode.NUMPAD8;
            public override VirtualKeyCode right => VirtualKeyCode.NUMPAD6;
            public override VirtualKeyCode press => VirtualKeyCode.NUMPAD0;
            public override VirtualKeyCode escape => VirtualKeyCode.ESCAPE;
            public override VirtualKeyCode back => VirtualKeyCode.DELETE;
        }

        private static readonly LeftConfig leftConfig = new LeftConfig();
        private static readonly RightConfig rightConfig = new RightConfig();

        private int leftPrevious, rightPrevious;

        private void ReadMessagesToEnd(Stream peer)
        {
            var rdr = new StreamReader(peer);
            while (true)
            {
                int r;
                try
                {
                    r = rdr.Read(); //Blocking call
                }
                catch (IOException ioex)
                {
                    Console.WriteLine("Connection closed hard! (read)");
                    Console.WriteLine(ioex);
                    break;
                }
                if (r == -1)
                {
                    Console.WriteLine("Connection closed ! (-1 read)");
                    break;
                }
                else
                {
                    bool rightBooster = (r & 1) != 0;

                    SideConfig sc;
                    if(rightBooster)
                    {
                        sc = rightConfig;
                    }
                    else
                    {
                        sc = leftConfig;
                    }

                    int previous = rightBooster ? rightPrevious : leftPrevious;

                    bool BitDiff(int from, int to, int bitPos, out int diff)
                    {
                        int fromMasked = (from & (1 << bitPos)) != 0 ? 1 : 0;
                        int toMasked = (to & (1 << bitPos)) != 0 ? 1 : 0;
                        diff = toMasked - fromMasked;
                        return diff != 0;
                    }

                    void DownUpKey(VirtualKeyCode vkc, int diff)
                    {
                        if (diff == 1)
                        {
                            InputSimulator.Keyboard.KeyDown(vkc);
                        }
                        else
                        {
                            InputSimulator.Keyboard.KeyUp(vkc);
                        }
                    }

                    if (BitDiff(previous, r, 1, out int pressed))
                    {
                        DownUpKey(sc.press, pressed);
                    }
                    if (BitDiff(previous, r, 2, out int right))
                    {
                        DownUpKey(sc.right, right);
                    }
                    if (BitDiff(previous, r, 3, out int up))
                    {
                        DownUpKey(sc.up, up);
                    }
                    if (BitDiff(previous, r, 4, out int down))
                    {
                        DownUpKey(sc.down, down);
                    }
                    if (BitDiff(previous, r, 5, out int left))
                    {
                        DownUpKey(sc.left, left);
                    }
                    if (BitDiff(previous, r, 6, out int back))
                    {
                        DownUpKey(sc.escape, back);
                    }
                    if (BitDiff(previous, r, 7, out int escape))
                    {
                        DownUpKey(sc.escape, escape);
                    }

                    if(rightBooster)
                    {
                        rightPrevious = r;
                    }
                    else
                    {
                        leftPrevious = r;
                    }
                }
                //Console.WriteLine($"Message : {Convert.ToString(r, 2).PadLeft(32)}");
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