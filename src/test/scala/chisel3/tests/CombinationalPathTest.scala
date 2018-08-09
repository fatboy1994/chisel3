package chisel3.tests

import org.scalatest._

import chisel3._
import chisel3.tester._

class CombinationalPathTest extends FlatSpec with ChiselScalatestTester {
  behavior of "Testers2"

  it should "detect combinationally-dependent operations across threads" in {
    assertThrows[ThreadOrderDependentException] {
      test(new PassthroughModule(Bool())) { c =>
        c.io.in.poke(true.B)
        fork {
          c.io.out.expect(true.B)
        }
      }
    }
  }

  it should "detect combinationally-dependent operations when a poke is active" in {
    assertThrows[ThreadOrderDependentException] {
      test(new PassthroughModule(Bool())) { c =>
        fork {
          c.io.in.poke(true.B)
          c.clock.step(2)
        } .fork {
          c.clock.step(1)
          c.io.out.expect(true.B)
        } .join
      }
    }
  }

  it should "detect combinationally-dependent operations on timescope reverts" in {
    assertThrows[ThreadOrderDependentException] {
      test(new PassthroughModule(Bool())) { c =>
        fork {
          c.io.in.poke(true.B)
          c.clock.step(1)
        } .fork {
          c.clock.step(1)
          c.io.out.expect(true.B)
        } .join
      }
    }
  }

  it should "allow higher priority combinationally-dependent operations" in {
    test(new PassthroughModule(Bool())) { c =>
      c.io.in.weakPoke(false.B)
      fork {
        c.io.in.poke(true.B)
        c.io.out.expect(true.B)
        c.clock.step(1)
      } .join
    }
  }

  it should "detect combinationally-dependent lower priority poke after a higher priority poke ends" in {
    assertThrows[ThreadOrderDependentException] {
      test(new PassthroughModule(Bool())) { c =>
        c.io.in.weakPoke(false.B)
        fork {
          c.io.in.poke(true.B)
          c.clock.step(1)
        } .fork {
          c.clock.step(1)
          c.io.out.expect(true.B)
        } .join
      }
    }
  }

  it should "detect combinationally-dependent operations through internal modules" in {
    assertThrows[ThreadOrderDependentException] {
      test(new Module {
        val io = IO(new Bundle {
          val in = Input(Bool())
          val out = Output(Bool())
        })
        val innerModule = Module(new PassthroughModule(Bool()))
        innerModule.io.in := io.in
        io.out := innerModule.io.out
      }) { c =>
        c.io.in.poke(true.B)
        fork {
          c.io.out.expect(true.B)
        }
      }
    }
  }

  it should "detect combinational paths across operations" in {
    assertThrows[ThreadOrderDependentException] {
      test(new Module {
        val io = IO(new Bundle {
          val in1 = Input(Bool())
          val in2 = Input(Bool())
          val out = Output(Bool())
        })
        io.out := io.in1 || io.in2
      }) { c =>
        c.io.in1.poke(true.B)
        fork {
          c.io.out.expect(true.B)
        }
      }
    }
  }
}