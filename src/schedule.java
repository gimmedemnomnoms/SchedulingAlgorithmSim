import java.util.ArrayList;
import java.util.List;
import java.io.*;
public class schedule {
    static List<Process> procs = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Please provide the process file and schedule file");
        }
        String processFile = args[0];
        String scheduleFile = args[1];
        //read in process information from file and store in list
        procs = readProcesses(processFile);
        //read in files with information about schedule
        readScheduleType(scheduleFile);
    }
    // function to print out stats for each process after running the simulation
    public static void printProcessStats() {
        double mtt = 0;
        double mntt = 0;
        double mart = 0;
        for (Process p : procs) {
            System.out.println("Process " + p.pid);
            System.out.println("\tStart Time: " + p.stats.startTime);
            System.out.println("\tFinish Time: " + p.stats.finishTime);
            System.out.println("\tService Time: " + p.stats.serviceTime);
            System.out.println("\tTurnaround Time " + p.stats.turnaroundTime());
            System.out.println("\tNormalized Turnaround Time: " + (double) p.stats.turnaroundTime() / p.stats.serviceTime);
            System.out.println("\tAverage Response Time: " + p.stats.avgResponseTime());
            mtt += p.stats.turnaroundTime();
            mntt = mntt + p.stats.turnaroundTime() / p.stats.serviceTime;
            mart += p.stats.avgResponseTime();
        }
        // calculate mean values for all processes
        System.out.println("--------------------------");
        System.out.println("Mean Turnaround Time: " + mtt / procs.size());
        System.out.println("Mean Normalized Turnaround Time: " + mntt / procs.size());
        System.out.println("Mean Response Time: " + mart / procs.size());

    }

    public static void readScheduleType(String scheduleFile) throws IOException {
        //FileReader in = new FileReader("./src/SPN.sf");
        //BufferedReader reader = new BufferedReader(in);
        //String scheduleType = reader.readLine();
        File file = new File(scheduleFile);
        if (!file.exists()) {
            System.out.println("File not found");
        }
        try (FileReader in = new FileReader(file); BufferedReader reader = new BufferedReader(in)) {
            String scheduleType = reader.readLine();
            // read and store different variables based on different schedule types, and run simulation for that schedule type
            switch (scheduleType) {
                case "FCFS":
                    FCFSSched fcfsSched = new FCFSSched(procs);
                    fcfsSched.runSim();
                    printProcessStats();
                    break;
                case "RR":
                    String quantumLine = reader.readLine();
                    // extract quantum value from line
                    int quantum = Integer.parseInt(quantumLine.substring(quantumLine.lastIndexOf("=") + 1));
                    RRSched rrSched = new RRSched(procs, quantum);
                    rrSched.runRRSim();
                    printProcessStats();
                    break;
                case "FEEDBACK":
                    String prioritiesLine = reader.readLine();
                    // extract number of priorities from line
                    int numPriorities = Integer.parseInt(prioritiesLine.substring(prioritiesLine.lastIndexOf("=") + 1));
                    quantumLine = reader.readLine();
                    // extract quantum value from line
                    quantum = Integer.parseInt(quantumLine.substring(quantumLine.lastIndexOf("=") + 1));
                    FBSched fbSched = new FBSched(procs, numPriorities, quantum);
                    fbSched.runFBSim();
                    printProcessStats();
                    break;
                case "SPN":
                    String service = reader.readLine();
                    //extract boolean value of serviceGiven
                    boolean serviceGiven = Boolean.parseBoolean(service.substring(service.lastIndexOf("=") + 1));
                    String alphaLine = reader.readLine();
                    // extract alpha value
                    double alpha = Double.parseDouble(alphaLine.substring(alphaLine.lastIndexOf("=") + 1));
                    SPNSched spnSched = new SPNSched(procs, serviceGiven, alpha);
                    spnSched.runSim();
                    printProcessStats();
                    break;
            }

        }
    }
    public static List<Process> readProcesses(String processFile) throws IOException {
        //FileReader in = new FileReader("./src/medium.txt");
        //BufferedReader reader = new BufferedReader(in);

        File file = new File(processFile);
        if (!file.exists()) {
            System.out.println("File not found");
        }

        try (FileReader in = new FileReader(file); BufferedReader reader = new BufferedReader(in)) {
            String lineRead;
            int processNum = 0;
            while ((lineRead = reader.readLine()) != null) {
                String[] splitLine = lineRead.split(" ");
                //pass process information to function to create process and add to list
                loadProcess(procs, processNum, splitLine);
                // counter used to name processes
                processNum++;
            }
            return procs;
        }
    }
    public static List<Process> loadProcess(List<Process> procs, int processNum, String [] splitLine){
        Process currentProcess = new Process(processNum);
        // first value in the line is the arrival time, store in variable
        currentProcess.arrive = Integer.parseInt(splitLine[0]);
        // rest of values are activity times, store in list
        for (int i = 1; i < splitLine.length; i++) {
            currentProcess.activities.add(Integer.valueOf(splitLine[i]));
        }
        // add process to list
        procs.add(currentProcess);
        return procs;
    }

    static class Stats {
        int serviceTime = 0;
        Integer startTime = null;
        Integer finishTime = null;
        int totalResponseTime = 0;
        int numResponseTimes = 0;
        Integer lastReady = null;
        int arriveTime;

        public Stats(int arriveTime) {
            this.arriveTime = arriveTime;
        }
        int turnaroundTime() {
            return finishTime - arriveTime;
        }
        double avgResponseTime() {
            if (numResponseTimes == 0) {
                return 0;
            }
            else {
                return (double) totalResponseTime / numResponseTimes;
            }
        }
    }

    static class Process {
        Stats stats = null; // to be added by scheduler
        int pid;
        int arrive;
        // used to differentiate a process timing out from blocking
        boolean timedOut = false;
        // holds value for CPU and I/O activity times
        List<Integer> activities = new ArrayList<>();
        // holds number of queues for feedback schedule
        int queueNum = 0;
        // holds s for SPN schedule
        double sVal = 0;

        public Process(int pid) {
            this.pid = pid;
        }

       @Override
        public String toString() {
            return "Process " + pid + ", Arrive " + arrive + ": " + activities;
        }

    }
    enum EventType {
        ARRIVAL, UNBLOCK
    }

    static class Event implements Comparable<Event> {
        // an event has a type, associated process, and the time it happens
        EventType type;
        Process process;
        int time;

        public Event(EventType type, Process process, int time) {
            this.type = type;
            this.process = process;
            this.time = time;
        }

        @Override
        public int compareTo(Event other) {
            if (this.time == other.time) {
                // break tie with event type
                if (this.type == other.type) {
                    // break tie by pid
                    return Integer.compare(this.process.pid, other.process.pid);
                }
                else {
                    return Integer.compare(this.type.ordinal(), other.type.ordinal());
                }
            }
            else {
                return Integer.compare(this.time, other.time);
            }
        }

        @Override
        public String toString() {
            return "At time " + time + ", " + type + " Event for Process " + process.pid;
        }
    }

    // a priority queue to sort events
    static class EventQueue {
        List<Event> queue = new ArrayList<>();
        boolean dirty = false;

        void push(Event item ) {
            if (item != null) {
                queue.add(item);
                dirty = true;
            }
            else {
                throw new IllegalArgumentException("Only Events allowed in queue");
            }
        }
        private void prepareLookup(String operation) {
            if (queue.isEmpty()) {
                throw new IllegalStateException(operation + " on empty EventQueue");
            }
            if (dirty) {
                queue.sort(Event::compareTo);
                dirty = false;
            }
        }
        Event pop() {
            prepareLookup("Pop");
            return queue.remove(0);
        }

        Event peek() {
            prepareLookup("Peek");
            return queue.get(0);
        }

        boolean hasEvent() {
            return !queue.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder tmp = new StringBuilder("EventQueue(");
            if (!queue.isEmpty()) {
                tmp.append(queue.get(0));
            }
            for (Event e : queue.subList(1, queue.size())) {
                tmp.append("; ").append(e);
            }
            tmp.append(")");
            return tmp.toString();
        }
    }

    static class RRSched {
        List<Process> procs;
        List<Process> rq;
        int clock;
        EventQueue eventQueue;
        int runningTime;
        Process running;
        int quantum;


        RRSched(List<Process> procs, int quantum) {
            this.procs = procs;
            this.quantum = quantum;
            this.rq = new ArrayList<>();
            this.clock = 0;
            this.eventQueue = new EventQueue();
            this.runningTime = 0;
            this.running = null;

            for (Process p : this.procs) {
                this.eventQueue.push(new Event(EventType.ARRIVAL, p, p.arrive));
                p.stats = new Stats(p.arrive);
            }
            // before running, creates a new activity list that splits CPU activity times by the quantum and adds a wait of 0 immediately after
            // to simulate round robin with a time quantum
            for (Process p : this.procs){
                List<Integer> moddedactivities = new ArrayList<>();
                for (int i = 0; i < p.activities.size(); i++){
                    // even index indicates CPU activity
                    if (i % 2 == 0) {
                        // if the CPU activity time is larger than the quantum, divide it into quantum sizes
                        if(p.activities.get(i) > this.quantum) {
                            // holds the "remaining" activity time after removing one quantum value at a time
                            int rem = p.activities.get(i);
                            while (rem > this.quantum) {
                                // add a quantum size activity to the list
                                moddedactivities.add(this.quantum);
                                rem -= this.quantum;
                                // add a "wait" of 0 to indicate a time out
                                moddedactivities.add(0);
                            }
                            // once the remainder is less than the quantum size, add to list
                            moddedactivities.add(rem);
                        }
                        // the CPU activity time is less than the quantum value, does not need to be split
                        else {
                            moddedactivities.add(p.activities.get(i));
                        }
                    }
                    // odd indexes are block time and do not need to be split
                    else {
                        moddedactivities.add(p.activities.get(i));
                    }
                }
                // reassign variable to hold the new list
                p.activities = moddedactivities;
            }

        }
        void debug (String msg) {
            System.out.println("[" + this.clock + "]" + " " + msg);
        }

        void runRRSim() {
            while ((this.running != null) || this.eventQueue.hasEvent()) {
                int nextEvent;
                if (this.eventQueue.hasEvent()){
                    nextEvent = this.eventQueue.peek().time;
                }
                else {
                    nextEvent = -1;
                }
                if ((this.running != null) && (nextEvent == -1 || nextEvent > this.runningTime + this.clock)){
                    this.clock = this.clock + this.runningTime;
                    this.runningTime = 0;
                }
                else {
                    if (this.running != null) {
                        this.runningTime -= nextEvent - this.clock;
                    }
                    this.clock = nextEvent;
                }
                if ((this.running != null) && this.runningTime == 0) {
                    if (this.running.activities.isEmpty()) {
                        this.debug("Process " + this.running.pid + " is exiting");
                        this.running.stats.finishTime = this.clock;
                    }
                    else {
                        int time = this.running.activities.remove(0);
                        // if the block time is 0, then the process timed out
                        if (time == 0){
                            this.debug("Process " + this.running.pid + " timed out");
                            this.running.timedOut = true;
                        }
                        // if not 0, then block
                        else {
                            this.debug("Process " + this.running.pid + " is blocking for " + time + " time units");
                        }
                        this.eventQueue.push(new Event(EventType.UNBLOCK, this.running, this.clock + time));
                    }
                    this.running = null;
                }
                // Do all events that happen at the same time
                while (this.eventQueue.hasEvent() && this.eventQueue.peek().time == this.clock) {
                    Event e = this.eventQueue.pop();
                    Process p = e.process;
                    if (e.type == EventType.ARRIVAL) {
                        this.debug("Process " + p.pid + " arrives");
                        this.rq.add(p);
                        p.stats.lastReady = this.clock;
                    }
                    else {
                        // if the process was timed out, reset
                        if (p.timedOut){
                            p.timedOut = false;
                        }
                        // only prints when a process was truly blocked, not timed out
                        else {
                            this.debug("Process " + p.pid + " unblocks");
                        }
                        this.rq.add(p);
                        p.stats.lastReady = this.clock;
                    }
                }
                if (this.running == null && !this.rq.isEmpty()) {
                    this.debug("Current Ready Queue: " + this.rq);
                    Process p = this.rq.remove(0);
                    p.stats.totalResponseTime = p.stats.totalResponseTime + (this.clock - p.stats.lastReady);
                    p.stats.numResponseTimes = p.stats.numResponseTimes + 1;
                    int cpuTime = p.activities.remove(0);
                    p.stats.serviceTime = p.stats.serviceTime + cpuTime;
                    if (p.stats.startTime == null) {
                        p.stats.startTime = this.clock;
                    }
                    this.runningTime = cpuTime;
                    this.running = p;
                    this.debug("Dispatching process " + p.pid);
                }
            }
            System.out.println("\n\nSimulation Complete\n-----------------------");
        }
    }

    static class FBSched {
        List<Process> procs;
        List<List<Process>> queues;
        List<Process> rq;
        int clock;
        EventQueue eventQueue;
        int runningTime;
        Process running;
        int quantum;
        int num_priorities;

        FBSched(List<Process> procs, int num_priorities, int quantum) {
            this.procs = procs;
            this.num_priorities = num_priorities;
            this.quantum = quantum;
            this.queues = new ArrayList<>();
            this.rq = new ArrayList<>();
            this.clock = 0;
            this.eventQueue = new EventQueue();
            this.runningTime = 0;
            this.running = null;

            // create a queue for each priority level
            for (int i = 0; i < num_priorities; i++){
                this.queues.add(new ArrayList<>());
             }

            for (Process p : this.procs) {
                this.eventQueue.push(new Event(EventType.ARRIVAL, p, p.arrive));
                p.stats = new Stats(p.arrive);
            }
            // before running, creates a new activity list that splits CPU activity times by the quantum and adds a wait of 0 immediately after
            // to simulate Feedback with a time quantum
            for (Process p : this.procs){
                List<Integer> moddedactivities = new ArrayList<>();
                for (int i = 0; i < p.activities.size(); i++){
                    // even index indicates CPU activity
                    if (i % 2 == 0) {
                        // if the CPU activity time is larger than the quantum, divide it into quantum sizes
                        if(p.activities.get(i) > quantum) {
                            int rem = p.activities.get(i);
                            // holds the "remaining" activity time after removing one quantum value at a time
                            while (rem > quantum) {
                                // add a quantum size activity to the list
                                moddedactivities.add(quantum);
                                rem -= quantum;
                                // add a "wait" of 0 to indicate a time out
                                moddedactivities.add(0);
                            }
                            // once the remainder is less than the quantum size, add to list
                            moddedactivities.add(rem);
                        }
                        // the CPU activity time is less than the quantum value, does not need to be split
                        else {
                            moddedactivities.add(p.activities.get(i));
                        }
                    }
                    // odd indexes are block time and do not need to be split
                    else {
                        moddedactivities.add(p.activities.get(i));
                    }
                }
                // reassign variable to hold the new list
                p.activities = moddedactivities;
            }

        }

        void debug (String msg) {
            System.out.println("[" + this.clock + "]" + " " + msg);
        }
        void runFBSim() {
            while ((this.running != null) || this.eventQueue.hasEvent()) {
                int nextEvent;
                if (this.eventQueue.hasEvent()){
                    nextEvent = this.eventQueue.peek().time;
                }
                else {
                    nextEvent = -1;
                }
                if ((this.running != null) && (nextEvent == -1 || nextEvent > this.runningTime + this.clock)){
                    this.clock = this.clock + this.runningTime;
                    this.runningTime = 0;
                }
                else {
                    if (this.running != null) {
                        this.runningTime -= nextEvent - this.clock;
                    }
                    this.clock = nextEvent;
                }
                if ((this.running != null) && this.runningTime == 0) {
                    if (this.running.activities.isEmpty()) {
                        this.debug("Process " + this.running.pid + " is exiting");
                        this.running.stats.finishTime = this.clock;
                    }
                    else {
                        int time = this.running.activities.remove(0);
                        // if the block time is 0, then the process timed out
                        if (time == 0){
                            this.debug("Process " + this.running.pid + " timed out");
                            this.running.timedOut = true;
                        }
                        // if not 0, then block
                        else {
                            this.debug("Process " + this.running.pid + " is blocking for " + time + " time units");
                        }
                        this.eventQueue.push(new Event(EventType.UNBLOCK, this.running, this.clock + time));
                    }
                    this.running = null;
                }
                // Do all events that happen at the same time
                while (this.eventQueue.hasEvent() && this.eventQueue.peek().time == this.clock) {
                    Event e = this.eventQueue.pop();
                    Process p = e.process;
                    if (e.type == EventType.ARRIVAL) {
                        this.debug("Process " + p.pid + " arrives");
                        this.queues.get(0).add(p);
                        p.stats.lastReady = this.clock;
                    }
                    else {
                        // if the process was timed out, reset

                        if (p.timedOut) {
                            p.timedOut = false;
                        }
                        // only prints when a process was truly blocked, not timed out
                        else {
                            this.debug("Process " + p.pid + " unblocks");
                        }
                        // lower the priority number if applicable
                        if (p.queueNum < num_priorities - 1) {
                            p.queueNum += 1;
                        }
                        this.queues.get(p.queueNum).add(p);
                        p.stats.lastReady = this.clock;
                    }
                }
                if (this.running == null && !this.queues.isEmpty()) {
                    // starting with the highest priority queue, look for next process
                    for (int i = 0; i < this.num_priorities; i++) {
                        if (!this.queues.get(i).isEmpty()) {
                            Process p = this.queues.get(i).remove(0);
                            p.stats.totalResponseTime = p.stats.totalResponseTime + (this.clock - p.stats.lastReady);
                            p.stats.numResponseTimes = p.stats.numResponseTimes + 1;
                            int cpuTime;
                            if (p.activities.get(0) < this.quantum) {
                                cpuTime = p.activities.get(0);
                            }
                            else {
                                cpuTime = this.quantum;
                            }
                            p.activities.remove(0);
                            this.runningTime = cpuTime;
                            p.stats.serviceTime = p.stats.serviceTime + cpuTime;
                            this.running = p;
                            if (p.stats.startTime == null){
                                p.stats.startTime = this.clock;
                            }
                            this.debug("Dispatching Process " + p.pid + " from queue " + i);
                            break;
                        }
                    }
                }
            }
            System.out.println("\n\nSimulation Complete\n-----------------------");
        }

    }
    static class FCFSSched {
        List<Process> procs;
        List<Process> rq;
        int clock;
        EventQueue eventQueue;
        int runningTime;
        Process running;

        FCFSSched(List<Process> procs) {
            this.procs = procs;
            this.rq = new ArrayList<>();
            this.clock = 0;
            this.eventQueue = new EventQueue();
            this.runningTime = 0;
            this.running = null;
            for (Process p : this.procs) {
                this.eventQueue.push(new Event(EventType.ARRIVAL, p, p.arrive));
                p.stats = new Stats(p.arrive);
            }
        }

        void debug (String msg) {
            System.out.println("[" + this.clock + "]" + " " + msg);
        }

        void runSim() {
            while ((this.running != null) || this.eventQueue.hasEvent()) {
                int nextEvent;
                if (this.eventQueue.hasEvent()){
                    nextEvent = this.eventQueue.peek().time;
                }
                else {
                    nextEvent = -1;
                }
                if ((this.running != null) && (nextEvent == -1 || nextEvent > this.runningTime + this.clock)){
                    this.clock = this.clock + this.runningTime;
                    this.runningTime = 0;
                }
                else {
                    if (this.running != null) {
                        this.runningTime -= nextEvent - this.clock;
                    }
                    this.clock = nextEvent;
                }
                if ((this.running != null) && this.runningTime == 0) {
                    if (this.running.activities.isEmpty()) {
                        this.debug("Process " + this.running.pid + " is exiting");
                        this.running.stats.finishTime = this.clock;
                    }
                    else {
                        int time = this.running.activities.remove(0);
                        this.debug("Process " + this.running.pid + " is blocking for " + time + " time units");
                        this.eventQueue.push(new Event(EventType.UNBLOCK, this.running, this.clock + time));
                    }
                    this.running = null;
                }
                // Do all events that happen at the same time
                while (this.eventQueue.hasEvent() && this.eventQueue.peek().time == this.clock) {
                    Event e = this.eventQueue.pop();
                    Process p = e.process;
                    if (e.type == EventType.ARRIVAL) {
                        this.debug("Process " + p.pid + " arrives");
                        this.rq.add(p);
                        p.stats.lastReady = this.clock;
                    }
                    else {
                        this.debug("Process " + p.pid + " unblocks");
                        this.rq.add(p);
                        p.stats.lastReady = this.clock;
                    }
                }
                if (this.running == null && !this.rq.isEmpty()) {
                    this.debug("Current Ready Queue: " + this.rq);
                    Process p = this.rq.remove(0);
                    p.stats.totalResponseTime = p.stats.totalResponseTime + (this.clock - p.stats.lastReady);
                    p.stats.numResponseTimes = p.stats.numResponseTimes + 1;
                    int cpuTime = p.activities.remove(0);
                    p.stats.serviceTime = p.stats.serviceTime + cpuTime;
                    if (p.stats.startTime == null) {
                        p.stats.startTime = this.clock;
                    }
                    this.runningTime = cpuTime;
                    this.running = p;
                    this.debug("Dispatching process " + p.pid);
                }
            }
            System.out.println("\n\nSimulation Complete\n-----------------------");
        }
    }

    static class SPNSched {
        List<Process> procs;
        List<Process> rq;
        int clock;
        EventQueue eventQueue;
        int runningTime;
        Process running;
        boolean serviceGiven;
        double alpha;


        SPNSched(List<Process> procs, boolean serviceGiven, double alpha) {
            this.procs = procs;
            this.serviceGiven = serviceGiven;
            this.alpha = alpha;
            this.rq = new ArrayList<>();
            this.clock = 0;
            this.eventQueue = new EventQueue();
            this.runningTime = 0;
            this.running = null;

            for (Process p : this.procs) {
                this.eventQueue.push(new Event(EventType.ARRIVAL, p, p.arrive));
                p.stats = new Stats(p.arrive);
            }
        }

        void debug (String msg) {
            System.out.println("[" + this.clock + "]" + " " + msg);
        }

        void runSim() {
            while ((this.running != null) || this.eventQueue.hasEvent()) {
                int nextEvent;
                if (this.eventQueue.hasEvent()){
                    nextEvent = this.eventQueue.peek().time;
                }
                else {
                    nextEvent = -1;
                }
                if ((this.running != null) && (nextEvent == -1 || nextEvent > this.runningTime + this.clock)){
                    this.clock = this.clock + this.runningTime;
                    this.runningTime = 0;
                }
                else {
                    if (this.running != null) {
                        this.runningTime -= nextEvent - this.clock;
                    }
                    this.clock = nextEvent;
                }
                if ((this.running != null) && this.runningTime == 0) {
                    if (this.running.activities.isEmpty()) {
                        this.debug("Process " + this.running.pid + " is exiting");
                        this.running.stats.finishTime = this.clock;
                    }
                    else {
                        int time = this.running.activities.remove(0);
                        this.debug("Process " + this.running.pid + " is blocking for " + time + " time units");
                        this.eventQueue.push(new Event(EventType.UNBLOCK, this.running, this.clock + time));
                    }
                    this.running = null;
                }
                // Do all events that happen at the same time
                while (this.eventQueue.hasEvent() && this.eventQueue.peek().time == this.clock) {
                    Event e = this.eventQueue.pop();
                    Process p = e.process;
                    if (e.type == EventType.ARRIVAL) {
                        this.debug("Process " + p.pid + " arrives");
                        this.rq.add(p);
                        p.stats.lastReady = this.clock;
                    }
                    else {
                        this.debug("Process " + p.pid + " unblocks");
                        this.rq.add(p);
                        p.stats.lastReady = this.clock;
                    }
                }
                if (this.running == null && !this.rq.isEmpty()) {
                    this.debug("Current Ready Queue: " + this.rq);
                    // calculate estimated service time for every process in the ready queue
                    for (Process p : rq) {
                        // if true, service time is duration of the first CPU activity
                        if (serviceGiven) {
                            if (p.sVal == 0) {
                                p.sVal = p.activities.get(0);
                            }
                        }
                        // if false, use exponential average to estimate
                        else {
                            int tVal = p.activities.get(0);
                            // use first CPU activity time for first calculation
                            if (p.sVal == 0) {
                                p.sVal = tVal;
                            } else {
                                p.sVal = alpha * tVal + (1 - alpha) * p.sVal;
                            }
                        }

                    }
                    // compares estimated service times, dispatches shortest
                    Process highestPriority = rq.get(0);
                    int index = 0;
                    for (int i = 0; i < rq.size(); i++) {
                        if (rq.get(i).sVal < highestPriority.sVal) {
                            highestPriority = rq.get(i);
                            index = i;
                        }
                    }
                    Process p = this.rq.remove(index);
                    p.stats.totalResponseTime = p.stats.totalResponseTime + (this.clock - p.stats.lastReady);
                    p.stats.numResponseTimes = p.stats.numResponseTimes + 1;
                    int cpuTime = p.activities.remove(0);
                    p.stats.serviceTime = p.stats.serviceTime + cpuTime;
                    if (p.stats.startTime == null) {
                        p.stats.startTime = this.clock;
                    }
                    this.runningTime = cpuTime;
                    this.running = p;
                    this.debug("Dispatching process " + p.pid);
                }
            }
            System.out.println("\n\nSimulation Complete\n-----------------------");
        }
    }

}