package edn.lakeopossmc.drivebysable.cable;

// --- A BLOCK HOLDING A SNAPSHOT THAT STILL NEEDS PINNING --- //
public interface WorldSpaceSnapshotHolder {

    // * Called every tick while queued, until it takes
    void tryBindWorldSpaceSnapshot();
}