using System;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using WindowsInput;
using WindowsInput.Native;

namespace GrooveCoasterController
{
    class Bluetooth2Key
    {
        private InputSimulator IS { get; }
        public Bluetooth2Key()
        {
            IS = new InputSimulator();
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
    }
}