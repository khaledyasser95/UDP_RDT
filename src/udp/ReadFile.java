package udp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadFile implements Runnable {
    String File;
    String protocol ;

    int bitsOfSqeunceNo ;

    int WINDOW_SIZE ;

    int TIMEOUT ;

    int MSS;
    public ReadFile(String file) throws IOException {
        File = file;
    }


    @Override
    public void run() {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(File));
            List<String> lines = new ArrayList<String>();

            String line_from_file;

            while ((line_from_file = reader.readLine()) != null) {
                lines.add(line_from_file.trim());
            }

            reader.close();
            String protocol = lines.get(0);

            int bitsOfSqeunceNo = Integer.parseInt(lines.get(1).charAt(0)+"");

            int WINDOW_SIZE = Integer.parseInt(lines.get(1).trim().charAt(2)+"");

            int TIMEOUT = Integer.parseInt(lines.get(2).trim().toString());

            int MSS = Integer.parseInt(lines.get(3).trim().toString());
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
