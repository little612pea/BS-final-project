package utils;

import entities.Product;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public final class RandomData {

    public static final List<String> categories = Arrays.asList("Computer Science", "Nature", "Philosophy",
            "History", "Autobiography", "Magazine", "Dictionary", "Novel", "Horror", "Others");
    public static final List<String> shop = Arrays.asList("Press-A", "Press-B", "Press-C", "Press-D",
            "Press-E", "Press-F", "Press-G", "Press-H");
    public static final List<String> authors = Arrays.asList("Nonehyo", "DouDou", "Coco", "Yuuku", "SoonWhy",
            "Fubuki", "Authentic", "Immortal", "ColaOtaku", "Erica", "ZaiZai", "DaDa", "Hgs");
    public static final List<String> titles = Arrays.asList("Database System Concepts", "Computer Networking",
            "Algorithms", "Database System Designs", "Compiler Designs", "C++ Primer", "Operating System",
            "The Old Man and the Sea", "How steel is made", "Le Petit Prince", "The Metamorphosis",
            "Miserable World", "Gone with the wind", "Eugenie Grandet", "Analysis of Dreams");
    public static final List<String> departments = Arrays.asList("Computer Science", "Law",
            "Management", "Civil Engineering", "Architecture", "Environmental Science",
            "English Language", "General Education", "Ideological & Political");

    private static final Calendar calStart = Calendar.getInstance();
    private static final Calendar calEnd = Calendar.getInstance();
    static {
        calStart.set(Calendar.YEAR, 2017);
        calEnd.set(Calendar.YEAR, 2023);
    }

}
