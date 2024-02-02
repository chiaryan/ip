import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Storage {

    private final String path;

    public Storage(String path) {
        this.path = path;
    }

    public TaskList loadTasks() throws DukeException {
        TaskList tasks = new TaskList();
        File f = new File(path);
        Scanner scanner = null;
        if (!f.exists()) {
            try {
                f.createNewFile();                
            } catch (IOException e) {
                throw new DukeException("IO error when creating data.txt: " + e.getMessage());
            }
        }
        try {
            scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
                String str = scanner.nextLine();
                tasks.add(Task.fromStorageString(str));
            }
        } catch (FileNotFoundException e) {
            throw new DukeException("IO error: " + e.getMessage());
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return tasks;
    }

    public void writeTasks(TaskList tasks) throws DukeException {
        PrintWriter pw = null;
        File f = new File(path);
        String toWrite = tasks.toStorageString();
        
        try {
            pw = new PrintWriter(new File(path));
            pw.write(toWrite);
            pw.close();
        } catch (FileNotFoundException e) {
            throw new DukeException("IO Error encountered while writing: " + e.getMessage());
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }
}