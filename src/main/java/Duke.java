import java.sql.Array;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Duke {
    
    /** Scanner for user input. */
    private static final Scanner input = new Scanner(System.in);
    
    private static boolean done = false;

    private static final HashMap<String, Command> commandMap = new HashMap<>();
    
    private static ArrayList<Task> taskList = new ArrayList<>();
    
    private static final Storage st = new Storage("data.txt");
    
    /**
     * Prints a message to the terminal, decorated with the Louie icon. 
     * Printing an empty string just outputs the icon
     *
     * @param message The message to print.
     */
    public static void print(String message) {
        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (String s : message.split("\n")) {
            sb.append("    ");
            if (isFirst) {
                sb.append("(>^.^<)");
                isFirst = false;
            } else {
                sb.append("       ");
            }
            sb.append(" ").append(s).append("\n");
        }
        sb.append("\n");
        System.out.print(sb);
    }
    
    public static void exit() {
        // Duke will exit at the end of the loop
        done = true;
    }

    public static void addCommand(String name, Consumer<String[]> executor) {
        commandMap.put(name, new Command(name, executor));
    }

    public static String taskStrings() {
        // here's your """""effectively final""""" value bro
        var numBox = new Object() {
            int num = 1;
        };
        return taskList.stream()
                .reduce(
                        new StringBuilder(),
                        (curr, acc) -> {
                            curr.append(numBox.num).append(".").append(acc.describe());
                            if (numBox.num < taskList.size()) {
                                curr.append("\n");
                            }
                            numBox.num++;
                            return curr;
                        },
                        StringBuilder::append)
                .toString();
    }
    public static void main(String[] mainArgs) {
        
        // initialisation
        Duke.addCommand("list", (args) -> {
            try {
                if (args.length > 1) {
                    throw new DukeOptionParsingException("option was not expected but was given: " + args[1]);
                }
                Duke.print("Here's what you've done today...\n" + taskStrings());
                
            } catch (DukeOptionParsingException e) {
                Duke.print("OH NYO ERROR!!!!!!!!!!!!! " + e.getMessage());
            }
        });
        
        Duke.addCommand("bye", (args) -> {
            try {
                if (args.length > 1) {
                    throw new DukeOptionParsingException("options were not expected but were given");
                }
                Duke.print("Ok, going to sleep...");
                Duke.exit();
            } catch (DukeOptionParsingException e) {
                Duke.print("OH NYO ERROR!!!!!!!!!!!!! " + e.getMessage());
            }
        });
        
        Duke.addCommand("mark", (args) -> {
            try {
                int i;
                Task t;
                
                try {
                    i = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e) {
                    throw new DukeOptionParsingException(
                            String.format("I expected a number but %s was given instead", args[1])
                    );
                } catch (IndexOutOfBoundsException e) {
                    throw new DukeOptionParsingException("command ended when an argument was expected");
                }
                
                if (args.length > 2) {
                    throw new DukeException("option was not expected but was given: " + args[2]);
                }
                
                try { 
                    t = taskList.get(i);
                } catch (IndexOutOfBoundsException e) {
                    throw new DukeException(
                            String.format("You tried to access an invalid task index: %s", args[1])
                    );
                }
                t.mark();
                Duke.print("CONGRATULATION!!!!!! you completed this task:\n" +
                        t.describe()
                );
                Duke.st.writeTasks(Duke.taskList);
            } catch (DukeException e) {
                Duke.print("OH NYO ERROR!!!!!!!!!!!!! " + e.getMessage());
            }
        });

        Duke.addCommand("unmark", (args) -> {
            try {
                int i;
                Task t;

                try {
                    i = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e) {
                    throw new DukeOptionParsingException(
                            String.format("I expected a number but %s was given instead", args[1])
                    );
                } catch (IndexOutOfBoundsException e) {
                    throw new DukeOptionParsingException("command ended when an argument was expected");
                }

                if (args.length > 2) {
                    throw new DukeException("option was not expected but was given: " + args[2]);
                }

                try {
                    t = taskList.get(i);
                } catch (IndexOutOfBoundsException e) {
                    throw new DukeException(
                            String.format("You tried to access an invalid task index: %s", args[1])
                    );
                }

                t.unmark();
                Duke.print("CONGRATULATION!!!!!! you un completed this task:\n" +
                        t.describe()
                );
                Duke.st.writeTasks(Duke.taskList);
            } catch (DukeException e) {
                Duke.print("OH NYO ERROR!!!!!!!!!!!!! " + e.getMessage());
            }
        });
        
        Duke.addCommand("todo", (args) -> {
            
            try {
                if (args.length <= 1) {
                    throw new DukeOptionParsingException("failed to specify a task to do");
                }
                String str = Arrays.stream(args)
                        .skip(1)
                        .collect(Collectors.joining(" "));

                var t = new ToDo(str);
                Duke.print(String.format("Ok, I've added a new todo...\n  %s", t.describe()));
                taskList.add(t);
                Duke.st.writeTasks(Duke.taskList);
            } catch (DukeException e) {
                Duke.print("OH NYO ERROR!!!!!!!!!!!!! " + e.getMessage());
            }
        });
        
        Duke.addCommand("deadline", (args) -> {
            StringBuilder by = new StringBuilder();
            StringBuilder name = new StringBuilder();
            Task t;
            
            final String NO_NAME = "you didn't specify specify a name for your deadline";
            final String NO_BY = "you failed to specify an end date using '/by'";
            
            try {
                int ctr = 1;
                for (; ; ctr++) {
                    if (ctr >= args.length) {
                        throw new DukeOptionParsingException(ctr == 1 ? NO_NAME : NO_BY);
                    }
                    if (args[ctr].startsWith("/")) {
                        break;
                    }
                    if (!name.isEmpty()) {
                        name.append(" ");
                    }
                    name.append(args[ctr]);
                }

                if (name.isEmpty()) {
                    throw new DukeOptionParsingException
                            (NO_NAME);
                }
                
                if (!args[ctr].equals("/by")) {
                    throw new DukeOptionParsingException
                            (String.format("I encountered an unexpected option '%s'", args[ctr]));
                }
                ctr++;

                for (; ctr < args.length; ctr++) {
                    if (args[ctr].startsWith("/")) {
                        throw new DukeOptionParsingException
                                (String.format("I encountered an unexpected option '%s'", args[ctr]));
                    }
                    if (!by.isEmpty()) {
                        by.append(" ");
                    }
                    by.append(args[ctr]);
                }

                if (by.isEmpty()) {
                    throw new DukeOptionParsingException(NO_BY);
                }
                
                try {
                    t = new Deadline(name.toString(), by.toString());
                } catch (DateTimeParseException e) {
                    throw new DukeException("Couldn't parse the end date " + by);
                }
                
                Duke.print(String.format("Ok, I've added a new deadline...\n  %s", t.describe()));
                Duke.taskList.add(t);
                Duke.st.writeTasks(Duke.taskList);
            } catch (DukeException e) {
                Duke.print("OH NYO ERROR!!!!!!!!!!!!! " + e.getMessage());
            }
            
            
        });
        
        Duke.addCommand("event", (args) -> {
            int state = 0;
            StringBuilder from = new StringBuilder();
            StringBuilder to = new StringBuilder();
            StringBuilder name = new StringBuilder();
            Event t;


            final String NO_NAME = "you didn't specify specify a name for your event";
            final String NO_FROM = "you failed to specify an start date using '/from'";
            final String NO_TO = "you failed to specify an end date using '/to'";

            try {
                int ctr = 1;
                for (; ; ctr++) {
                    if (ctr >= args.length) {
                        throw new DukeOptionParsingException(ctr == 1 ? NO_NAME : NO_FROM);
                    }
                    if (args[ctr].startsWith("/")) {
                        break;
                    }
                    if (!name.isEmpty()) {
                        name.append(" ");
                    }
                    name.append(args[ctr]);
                }

                if (name.isEmpty()) {
                    throw new DukeOptionParsingException
                            (NO_NAME);
                }

                if (!args[ctr].equals("/from")) {
                    throw new DukeOptionParsingException
                            (String.format("I encountered an unexpected option '%s'", args[ctr]));
                }
                ctr++;

                for (; ; ctr++) {
                    if (ctr >= args.length) {
                        throw new DukeOptionParsingException(NO_TO);
                    }
                    
                    if (args[ctr].startsWith("/")) {
                        break;
                    }
                    if (!from.isEmpty()) {
                        from.append(" ");
                    }
                    from.append(args[ctr]);
                }

                if (from.isEmpty()) {
                    throw new DukeOptionParsingException(NO_FROM);
                }
                
                if (!args[ctr].equals("/to")) {
                    throw new DukeOptionParsingException
                            (String.format("I encountered an unexpected option '%s'", args[ctr]));
                }
                ctr++;

                for (; ctr < args.length; ctr++) {
                    if (args[ctr].startsWith("/")) {
                        throw new DukeOptionParsingException
                                (String.format("I encountered an unexpected option '%s'", args[ctr]));
                    }
                    if (!to.isEmpty()) {
                        to.append(" ");
                    }
                    to.append(args[ctr]);
                }

                if (to.isEmpty()) {
                    throw new DukeOptionParsingException(NO_TO);
                }
                
                try {
                    t = new Event(name.toString(), from.toString(), to.toString());
                } catch (DateTimeParseException e) {
                    throw new DukeException(String.format
                            ("Couldn't parse the start/end date %s/%s", from, to));
                }

                Duke.print(String.format("Ok, I've added a new event...\n  %s", t.describe()));
                Duke.taskList.add(t);
                Duke.st.writeTasks(Duke.taskList);
            } catch (DukeException e) {
                Duke.print("OH NYO ERROR!!!!!!!!!!!!! " + e.getMessage());
            }
        });
        
        Duke.addCommand("delete", (args) -> {
            try {
                int i;
                Task t;

                try {
                    i = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e) {
                    throw new DukeOptionParsingException(
                            String.format("I expected a number but %s was given instead", args[1])
                    );
                } catch (IndexOutOfBoundsException e) {
                    throw new DukeOptionParsingException("command ended when an argument was expected");
                }

                if (args.length > 2) {
                    throw new DukeException("option was not expected but was given: " + args[2]);
                }
                
                if (i < 0 || i >= taskList.size()) {
                    throw new DukeException
                            (String.format("You tried to access an invalid task index: %s", args[1]));
                }
                t = taskList.remove(i);
                
                Duke.print
                        ("I'm deleting this task. bye...\n" +
                        t.describe());
                Duke.st.writeTasks(Duke.taskList);
            } catch (DukeException e) {
                Duke.print("OH NYO ERROR!!!!!!!!!!!!! " + e.getMessage());
            }
        });
        
        try {
            Duke.taskList = Duke.st.loadTasks();
        } catch (DukeException e) {
            Duke.print(String.format
                    ("Error loading task data: %s"
                            + "\n\nPlease delete 'data.txt' and try again. Goodbye...", e.getMessage()));
            System.exit(1);
        }
        
        Duke.print("Hello, my name is... Louie!!!!\n" + 
                   "What can I do for you today?");
        while (!done) {
            String str = input.nextLine();
            String[] args = str.split(" ");
            if (commandMap.containsKey(args[0])) {
                commandMap.get(args[0]).run(args);
            } else {
                Duke.print("no matching command...");
            }
        }
    }

}
