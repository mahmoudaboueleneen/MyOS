package main.elements;

import java.io.Serializable;

public enum ProcessState implements Serializable {
    NEW, READY, RUNNING, BLOCKED, FINISHED
}
