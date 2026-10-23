import com.bettercontent.betterdrinkingwater.fixture.ThirstRecursionFixIdentity;
import java.util.List;

/** Portable identity controls only; never represents installed/applied native Mixins. */
public final class ThirstRecursionFixIdentityControl {
    public static void main(String[] args) {
        String target = "dev.ghen.thirst.foundation.common.loot.AddLootTableModifier";
        String internal = target.replace('.', '/');
        String fix = ThirstRecursionFixIdentity.MIXIN_CLASS;
        check(ThirstRecursionFixIdentity.matches(fix, List.of(internal), target));
        check(!ThirstRecursionFixIdentity.matches("other.Fix", List.of(internal), target));
        check(!ThirstRecursionFixIdentity.matches(fix, List.of(), target));
        check(!ThirstRecursionFixIdentity.matches(fix, List.of("other/Target"), target));
        check(!ThirstRecursionFixIdentity.matches(fix, List.of(target), target));
        check(!ThirstRecursionFixIdentity.matches(fix, List.of(internal, "other/Target"), target));
        check(!ThirstRecursionFixIdentity.matches(fix, List.of(internal, internal), target));
        System.out.println("PASS seven primitive applied-identity/sole-target cases; NOT native application proof");
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Applied Mixin identity/sole-target check weakened");
    }
}
