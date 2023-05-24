package main.process_management;

import java.io.Serializable;

public enum ProcessState implements Serializable {
    NEW, READY, RUNNING, BLOCKED, FINISHED
}
