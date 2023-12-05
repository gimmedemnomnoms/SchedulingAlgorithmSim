import java.util.ArrayList;
import java.util.List;
import java.io.*;
public class Schedule {
    static List<Process> procs = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        procs = readProcesses();
        System.out.println("NEXT LINE IS PROCS");
        System.out.println(procs);
        //FCFSSched fcfsSched = new FCFSSched(procs);
        //fcfsSched.runSim();
        //printProcessStats();
        //RRSched rrSched = new RRSched(procs);
        //rrSched.runRRSim();
        //printProcessStats();
        //FBSched fbSched = new FBSched(procs);
        //fbSched.runFBSim();
        //printProcessStats();
        readScheduleType();


    }
    public static void printProcessStats() {
        double mtt = 0;
        double mntt = 0;
        double mart = 0;
        for (int i = 0; i < procs.size(); i++) {
            Process p = procs.get(i);
            System.out.println("Process " + p.pid);
            System.out.println("\tStart Time: " + p.stats.startTime);
            System.out.println("\tFinish Time: " + p.stats.finishTime);
            System.out.println("\tService Time: " + p.stats.serviceTime);
            System.out.println("\tTurnaround Time " + p.stats.turnaroundTime());
            System.out.println("\tNormalized Turnaround Time: " + (double) p.stats.turnaroundTime() / p.stats.serviceTime);
            System.out.println("\tAverage Response Time: " + p.stats.avgResponseTime());
            System.out.println("\t\tTotal response time: " + p.stats.totalResponseTime);
            System.out.println("\t\tNumber of response times: " + p.stats.numResponseTimes);
            mtt += p.stats.turnaroundTime();
            //mntt += p.stats.turnaroundTime()/p.stats.serviceTime;
            mart += p.stats.avgResponseTime();
        }
        System.out.println("--------------------------");
        System.out.println("Mean Turnaround Time: " + mtt / procs.size());
        System.out.println("Mean Normalized Turnaround Time: " + mntt / procs.size());
        System.out.println("Mean Response Time: " + mart / procs.size());

    }

    public static void readScheduleType() throws IOException {
        FileReader in = new FileReader("./src/SPN.sf");
        BufferedReader reader = new BufferedReader(in);
        String scheduleType = reader.readLine();
        System.out.println(scheduleType);
        switch (scheduleType) {
            case "FCFS":
                FCFSSched fcfsSched = new FCFSSched(procs);
                fcfsSched.runSim();
                printProcessStats();
                break;
            case "RR":
                String quantumLine = reader.readLine();
                int quantum = Integer.parseInt(quantumLine.substring(quantumLine.lastIndexOf("=") + 1));
                System.out.println(quantum);
                RRSched rrSched = new RRSched(procs, quantum);
                rrSched.runRRSim();
                printProcessStats();
                break;
            case "FEEDBACK":
                String prioritiesLine = reader.readLine();
                int numPriorities = Integer.parseInt(prioritiesLine.substring(prioritiesLine.lastIndexOf("=") + 1));
                quantumLine = reader.readLine();
                quantum = Integer.parseInt(quantumLine.substring(quantumLine.lastIndexOf("=") + 1));
                System.out.println(numPriorities);
                System.out.println(quantum);
                FBSched fbSched = new FBSched(procs, numPriorities, quantum);
                fbSched.runFBSim();
                printProcessStats();
                break;
            case "SPN":
                String service = reader.readLine();
                boolean serviceGiven = Boolean.parseBoolean(service.substring(service.lastIndexOf("=") + 1));
                String alphaLine = reader.readLine();
                double alpha = Double.parseDouble(alphaLine.substring(alphaLine.lastIndexOf("=") + 1));
                System.out.println(serviceGiven);
                System.out.println(alpha);
                break;
        }
    }
    public static List<Process> readProcesses() throws IOException {
        //File inputFile = new File("./src/medium.txt");
        FileReader in = new FileReader(new File("./src/medium.txt"));
        //FileInputStream fstream = new FileInputStream("C:\\Users\\alina\\Documents\\Classes\\Fall 2023\\Operating Systems\\Project 3\\scheduling\\src\\medium.txt");
        //DataInputStream input = new DataInputStream(fstream);
        BufferedReader reader = new BufferedReader(in);
        String lineRead;
        int processNum = 0;
        while ((lineRead = reader.readLine()) != null) {
            System.out.println(lineRead);
            String[] splitLine = lineRead.split(" ");
            loadProcess(procs, processNum, splitLine);
            processNum++;

        }
        return procs;
    }
    public static List<Process> loadProcess(List<Process> procs, int processNum, String [] splitLine){
        Process currentProcess = new Process(processNum);
        currentProcess.arrive = Integer.parseInt(splitLine[0]);
        for (int i = 1; i < splitLine.length; i++) {
            currentProcess.activities.add(Integer.valueOf(splitLine[i]));
        }
        System.out.println(currentProcess.activities);
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
        Stats stats = null;
        int pid;
        int arrive;
        List<Integer> activities = new ArrayList<>();
        int queueNum = 0;

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
                if (this.type == other.type) {
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

    static class EventQueue {
        List<Event> queue = new ArrayList<>();
        boolean dirty = false;

        void push(Event item ) {
            if (item instanceof Event) {
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

        boolean empty() {
            return queue.isEmpty();
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
            for (Process p : this.procs){
                System.out.println("PRINTING OUT ACTIVITIES MAYBE HOPEFULLY");
                List<Integer> moddedactivities = new ArrayList<>();
                for (int i = 0; i < p.activities.size(); i++){
                    System.out.println("PRINTING MODDED");
                    if (i % 2 == 0) {
                        if(p.activities.get(i) > this.quantum) {
                            int rem = p.activities.get(i);
                            while (rem > this.quantum) {
                                moddedactivities.add(this.quantum);
                                System.out.println("1Added " + this.quantum);
                                rem -= this.quantum;
                                moddedactivities.add(0);
                                System.out.println("added blocking for 0 ");
                            }
                            moddedactivities.add(rem);
                            System.out.println("added rest of rem");
                        }
                        else {
                            moddedactivities.add(p.activities.get(i));
                            System.out.println("2added " + p.activities.get(i));
                        }
                    }
                    else {
                        moddedactivities.add(p.activities.get(i));
                        System.out.println("3added " + p.activities.get(i));
                    }
                    System.out.println("PRINTING MODDED");
                    System.out.println(moddedactivities);
                }
                p.activities = moddedactivities;
                System.out.println("PRINTING OUT ACTIVITIES MAYBE HOPEFULLY");
                System.out.println(p.activities);
            }

        }
        void debug (String msg) {
            System.out.println(this.clock + " " + msg);
        }

        void runRRSim() {
            while ((this.running != null) || this.eventQueue.hasEvent()) {
                int nextEvent;
                if (this.eventQueue.hasEvent()){
                    //System.out.println("has more events");
                    nextEvent = this.eventQueue.peek().time;
                }
                else {
                    //System.out.println("no more events");
                    nextEvent = -1;
                }
                if ((this.running != null) && (nextEvent == -1 || nextEvent > this.runningTime + this.clock)){
                    //System.out.println("running and | no more events or next event time is larger than running time plus clock");
                    this.clock = this.clock + this.runningTime;
                    this.runningTime = 0;
                }
                else {
                    //System.out.println("else of: running and | no more events or next event time is larger than running time plus clock");
                    if (this.running != null) {
                        //System.out.println("running");
                        this.runningTime -= nextEvent - this.clock;
                    }
                    this.clock = nextEvent;
                }
                if ((this.running != null) && this.runningTime == 0) {
                    //System.out.println("running and running time is 0");
                    if (this.running.activities.isEmpty()) {
                        //System.out.println("no more activities");
                        this.debug("Process " + this.running.pid + " is exiting");
                        this.running.stats.finishTime = this.clock;
                    }
                    else {
                        //System.out.println("more activities");
                        int time = this.running.activities.remove(0);
                        this.debug("Process " + this.running.pid + " is blocking for " + time + " time units");
                        this.eventQueue.push(new Event(EventType.UNBLOCK, this.running, this.clock + time));
                    }
                    this.running = null;
                }
                while (this.eventQueue.hasEvent() && this.eventQueue.peek().time == this.clock) {
                    //System.out.println("while events and event queue time is equal to clock");
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
                    //System.out.println("if not running and rq is empty");
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

            for (int i = 0; i < num_priorities; i++){
                this.queues.add(new ArrayList<>());
             }

            for (Process p : this.procs) {
                this.eventQueue.push(new Event(EventType.ARRIVAL, p, p.arrive));
                p.stats = new Stats(p.arrive);
            }
            for (Process p : this.procs){
               // System.out.println("PRINTING OUT ACTIVITIES MAYBE HOPEFULLY");
                List<Integer> moddedactivities = new ArrayList<>();
                for (int i = 0; i < p.activities.size(); i++){
                    //System.out.println("PRINTING MODDED");
                    if (i % 2 == 0) {
                        if(p.activities.get(i) > quantum) {
                            int rem = p.activities.get(i);
                            while (rem > quantum) {
                                moddedactivities.add(quantum);
                               // System.out.println("1Added " + quantum);
                                rem -= quantum;
                                moddedactivities.add(0);
                                //System.out.println("added blocking for 0 ");
                            }
                            moddedactivities.add(rem);
                            //System.out.println("added rest of rem");
                        }
                        else {
                            moddedactivities.add(p.activities.get(i));
                            //System.out.println("2added " + p.activities.get(i));
                        }
                    }
                    else {
                        moddedactivities.add(p.activities.get(i));
                        //System.out.println("3added " + p.activities.get(i));
                    }
                    //System.out.println("PRINTING MODDED");
                    //System.out.println(moddedactivities);
                }
                p.activities = moddedactivities;
                //System.out.println("PRINTING OUT ACTIVITIES MAYBE HOPEFULLY");
                System.out.println(p.activities);
            }

        }

        void debug (String msg) {
            System.out.println(this.clock + " " + msg);
        }
        void runFBSim() {
            while ((this.running != null) || this.eventQueue.hasEvent()) {
                int nextEvent;
                if (this.eventQueue.hasEvent()){
                    //System.out.println("has more events");
                    nextEvent = this.eventQueue.peek().time;
                }
                else {
                    //System.out.println("no more events");
                    nextEvent = -1;
                }
                if ((this.running != null) && (nextEvent == -1 || nextEvent > this.runningTime + this.clock)){
                    //System.out.println("running and | no more events or next event time is larger than running time plus clock");
                    this.clock = this.clock + this.runningTime;
                    this.runningTime = 0;
                }
                else {
                    //System.out.println("else of: running and | no more events or next event time is larger than running time plus clock");
                    if (this.running != null) {
                        //System.out.println("running");
                        this.runningTime -= nextEvent - this.clock;
                    }
                    this.clock = nextEvent;
                }
                if ((this.running != null) && this.runningTime == 0) {
                    //System.out.println("running and running time is 0");
                    if (this.running.activities.isEmpty()) {
                        //System.out.println("no more activities");
                        this.debug("Process " + this.running.pid + " is exiting");
                        this.running.stats.finishTime = this.clock;
                    }
                    else {
                        //System.out.println("more activities");
                        int time = this.running.activities.remove(0);
                        this.debug("Process " + this.running.pid + " is blocking for " + time + " time units");
                        this.eventQueue.push(new Event(EventType.UNBLOCK, this.running, this.clock + time));
                    }
                    this.running = null;
                }
                while (this.eventQueue.hasEvent() && this.eventQueue.peek().time == this.clock) {
                    //System.out.println("while events and event queue time is equal to clock");
                    Event e = this.eventQueue.pop();
                    Process p = e.process;
                    if (e.type == EventType.ARRIVAL) {
                        this.debug("Process " + p.pid + " arrives");
                        this.queues.get(0).add(p);
                        p.stats.lastReady = this.clock;
                    }
                    else {
                        this.debug("Process " + p.pid + " unblocks");
                        if (p.queueNum < num_priorities - 1) {
                            p.queueNum += 1;
                        }
                        this.queues.get(p.queueNum).add(p);
                        p.stats.lastReady = this.clock;
                    }
                }
                if (this.running == null && !this.queues.isEmpty()) {
                    //System.out.println("if not running and rq is empty");
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
                            this.running = p;
                            this.debug("Dispatching " + p.pid + "from queue " + i);
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
                System.out.println("p.stats is " + p.arrive);
            }
        }

        void debug (String msg) {
            System.out.println(this.clock + " " + msg);
        }

        void runSim() {
            while ((this.running != null) || this.eventQueue.hasEvent()) {
                int nextEvent;
                if (this.eventQueue.hasEvent()){
                    System.out.println("has more events");
                    nextEvent = this.eventQueue.peek().time;
                }
                else {
                    System.out.println("no more events");
                    nextEvent = -1;
                }
                if ((this.running != null) && (nextEvent == -1 || nextEvent > this.runningTime + this.clock)){
                    System.out.println("running and | no more events or next event time is larger than running time plus clock");
                    this.clock = this.clock + this.runningTime;
                    this.runningTime = 0;
                }
                else {
                    System.out.println("else of: running and | no more events or next event time is larger than running time plus clock");
                    if (this.running != null) {
                        System.out.println("running");
                        this.runningTime -= nextEvent - this.clock;
                    }
                    this.clock = nextEvent;
                }
                if ((this.running != null) && this.runningTime == 0) {
                    System.out.println("running and running time is 0");
                    if (this.running.activities.isEmpty()) {
                        System.out.println("no more activities");
                        this.debug("Process " + this.running.pid + " is exiting");
                        this.running.stats.finishTime = this.clock;
                    }
                    else {
                        System.out.println("more activities");
                        int time = this.running.activities.remove(0);
                        this.debug("Process " + this.running.pid + " is blocking for " + time + " time units");
                        this.eventQueue.push(new Event(EventType.UNBLOCK, this.running, this.clock + time));
                    }
                    this.running = null;
                }
                while (this.eventQueue.hasEvent() && this.eventQueue.peek().time == this.clock) {
                    System.out.println("while events and event queue time is equal to clock");
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
                    System.out.println("if not running and rq is empty");
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

}