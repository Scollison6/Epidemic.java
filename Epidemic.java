// Programmed by Spencer Collison
// Error Class, createHistogram method credited to
// Douglas Jones from examples shown in class

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;

public class Epidemic {

    public static void main(String[] args) {
        Scanner scan = null;
        if (args.length < 1) {Error.fatal( "Missing file name argument\n" );}
        else try {scan = new Scanner(new File(args[0]));}
        catch ( FileNotFoundException e) {
            Error.fatal( "Can't open file: " + args[0] + "\n" );
        }


        execute(scan, args);


    }

    public static void execute(Scanner sc, String[] arg){
        
        ArrayList<Person> personDB = new ArrayList<Person>();
        ArrayList<House> houseDB = new ArrayList<House>();
        ArrayList<Employee> employeeDB = new ArrayList<Employee>();
        ArrayList<Workplace> workplaceDB = new ArrayList<Workplace>();
        int population = 0;
        int infected = 0;
        int bound = 20;
        double houseMedian = 0;
        double houseScatter = 0;
        double employmentProb = 0;
        double wpMedian = 0;
        double wpScatter = 0;
        Error er = new Error();

        Pattern delims = Pattern.compile(
				"([ \\t\\r\\n,;]|(//[\\S \\t]*\\n))*" );
        //doubleFinder pattern provided by Brian on stackoverflow
        //https://stackoverflow.com/questions/3681242/
        // java-how-to-parse-double-from-regex
        Pattern doubleFinder = Pattern.compile(
        "[+-]?[0-9]+(\\.[0-9]+)?([Ee][+-]?[0-9]+)?(\\.)?");
        Pattern intFinder = Pattern.compile("[-+]?[0-9]*");

        while(sc.hasNext()){
            String command = sc.next();
            if (command.equals("pop")){
                try {
                    sc.skip(delims);
                    sc.skip(intFinder);
                    population = Integer.parseInt(sc.match().group());
                    if (population < 1){
                        er.fatal(lazyFormat(
                        true, "Population needs to be greater than 0"));
                    }
                } catch (Exception e) {
                    er.fatal(lazyFormat(true,
                     "Population needs to be of type Integer"));
                }

            }
            else if (command.equals("house")){
                try {
                    sc.skip(delims);
                    sc.skip(doubleFinder);
                    houseMedian = Double.parseDouble(sc.match().group());
                    sc.skip(delims);
                    sc.skip(doubleFinder);
                    houseScatter = Double.parseDouble(sc.match().group());
                } catch (Exception e) {
                    er.fatal(lazyFormat(true,
                    "Median and Scatter Values for House must be Doubles"));
                }
                // check median and scatter for legitimacy
                if (houseMedian <= 0.0) {
                    er.fatal(lazyFormat(true,
                            ("House median " + houseMedian +
                            " must be positive" )));
                }
                if (houseScatter < 0.0) {
                    er.fatal(lazyFormat(true, ("scatter " + houseScatter +
                            " must not be negative")));
                }
            }
            else if (command.equals("infected")){
                try {
                    sc.skip(delims);
                    sc.skip(intFinder);
                    infected = Integer.parseInt(sc.match().group());
                    if (infected < 1){
                        er.fatal(lazyFormat(true,
                                "Infected needs to be greater than 0"));
                    }
                } catch (Exception e) {
                    er.fatal(lazyFormat(true,
                            "Infected needs to be of type Integer"));
                }
            }
            else if (command.equals("employed")){
                try{
                    sc.skip(delims);
                    sc.skip(doubleFinder);
                    employmentProb = Double.parseDouble(sc.match().group());
                    if (employmentProb < 0 || employmentProb > 1){
                        er.fatal(lazyFormat(
                        true, ("Employment probability needs" +
"to be between 0 and 1")));
                    }
                }catch(Exception e){
                    er.fatal(lazyFormat(true,
                            "Employment probability needs to of type Double"));
                }
            }
            else if (command.equals("workplace")){
                try {
                    sc.skip(delims);
                    sc.skip(doubleFinder);
                    wpMedian = Double.parseDouble(sc.match().group());
                    sc.skip(delims);
                    sc.skip(doubleFinder);
                    wpScatter = Double.parseDouble(sc.match().group());
                } catch (Exception e) {
                    er.fatal(lazyFormat(true, "Median and Scatter Values " +
                            "for Workplace must be Doubles"));
                }
                // check median and scatter for legitimacy
                if (wpMedian <= 0.0) {
                    er.fatal(lazyFormat(true,
                            ("median " + wpMedian + " must be positive")) );
                }
                if (wpScatter < 0.0) {
                    er.fatal(lazyFormat(true, ("scatter " + wpScatter +
                            " must not be negative")));
                }
            }
            sc.skip(delims);
        }

        populatePeople(personDB, population, employmentProb);
        infectPeople(personDB, infected);
        int[] peopleHistogram = createHistogram(houseMedian,
                            houseScatter, bound, population);
        int totalEmployees = findEmployees(personDB, employeeDB);
        int[] employeeHistogram = createHistogram(wpMedian,
                                wpScatter, bound, totalEmployees);
        createHouses(houseDB, bound, peopleHistogram);
        fillHouses(houseDB, personDB);
        createWorkplaces(workplaceDB, bound, employeeHistogram);
        fillWorkplaces(workplaceDB, employeeDB);


        printPeople(personDB);
        System.out.println("House Size Distribution");
        printHistogram(peopleHistogram, bound);
        printHouses(houseDB);
        System.out.println("Workplace Size Distribution");
        printHistogram(employeeHistogram, bound);
        printWorkplaces(workplaceDB);

    }

    public static String lazyFormat(
            boolean format, final String s) {
        if (format) {
            return String.format(s);
        }
        else {
            return null;
        }
    }

    public static void populatePeople(ArrayList<Person>
                        peopleData, int pop, double empProb){
        empProb = empProb * 100;
        Random rand = new Random();
        for(int i = 0; i < pop; i++){
            if(rand.nextInt(101) > empProb) {
                peopleData.add(new Person());
            }
            else{
                peopleData.add(new Employee());
            }
        }
    }

    public static int findEmployees(ArrayList<Person>
                    people, ArrayList<Employee> employees){
        for(int i = 0; i < people.size(); i++){
            if (people.get(i).getClass() == (new Employee()).getClass()){
                employees.add((Employee) people.get(i));
            }
        }
        return employees.size();
    }

    public static void infectPeople(ArrayList<Person>
                            peopleData, int infectTotal){
        Random rand = new Random();

        int i = 0;
        while (i < infectTotal){
            int personLocation = rand.nextInt(peopleData.size());
            while (peopleData.get(personLocation).getInfectionState() ==
                    Person.HealthStatus.latent){
                personLocation = rand.nextInt(peopleData.size());
            }
            peopleData.get(personLocation).setInfectionState(
                                    Person.HealthStatus.latent);
            i++;
        }
    }

    public static void createHouses(ArrayList<House> houseData,
                                    int bound, int[] histogram){

        for (int i = 0; i < bound; i++) {
            for (int j = 0; j < histogram[i]; j++ ) {
                houseData.add(new House(i));
            }
        }
    }

    public static void fillHouses(ArrayList<House> houses,
                                    ArrayList<Person> people){
        Random rand = new Random();
        int totalResidents = 0;

        for(int i = 0; i < houses.size(); i++){
            totalResidents += houses.get(i).placeSize;
        }

        while(totalResidents > people.size()){
            people.add(new Person());
        }

        for(int i = 0; i < houses.size(); i++){
            for(int j = 0; j < houses.get(i).placeSize; j++){
                int randomPerson = rand.nextInt(people.size());
                while(people.get(randomPerson).home != null) {
                    randomPerson = rand.nextInt(people.size());
                }
                people.get(randomPerson).home = houses.get(i);
                houses.get(i).residents.add(people.get(randomPerson));
            }
        }

    }

    public static void createWorkplaces(ArrayList<Workplace> wpData,
                                        int bound, int[] histogram){

        for (int i = 0; i < bound; i++) {
            for (int j = 0; j < histogram[i]; j++ ) {
                wpData.add(new Workplace(i));
            }
        }
    }

    public static void fillWorkplaces(ArrayList<Workplace> workplaces,
                                        ArrayList<Employee> employees){
        Random rand = new Random();
        int totalResidents = 0;

        for(int i = 0; i < workplaces.size(); i++){
            totalResidents += workplaces.get(i).placeSize;
        }

        while(totalResidents > employees.size()){
            employees.add(new Employee());
        }

        for(int i = 0; i < workplaces.size(); i++){
            for(int j = 0; j < workplaces.get(i).placeSize; j++){
                int randomPerson = rand.nextInt(employees.size());
                while(employees.get(randomPerson).wpLocation != null) {
                    randomPerson = rand.nextInt(employees.size());
                }
                employees.get(randomPerson).wpLocation = workplaces.get(i);
                workplaces.get(i).residents.add(employees.get(randomPerson));
            }
        }

    }

    public static int[] createHistogram(
            double median, double scatter, int bound, int population){
        Random rand = new Random();  // a source of random numbers

        // the histogram accumulator
        int [] histogram = new int[bound];

        // sigma is the standard deviation of the underlying normal distribution
        double sigma = Math.log( (scatter + median) / median );

        // do the experiment to generate the histogram
        int i = 0;
        while (i < population) {

            // draw a random number from a log normal distribution
            double lognormal = Math.exp( sigma * rand.nextGaussian() ) * median;

            // find what bin of the histogram it goes in and increment that bin
            int bin = (int)Math.ceil( lognormal );
            if (bin <= 0) bin = 0;
            if (bin >= bound) bin = bound - 1;
            histogram[bin]++;
            i += bin;
        }

        return histogram;
    }

    public static void printPeople(ArrayList<Person> peopleData){
        System.out.println("Person / House / Work* / Infection State");
        for(int j = 0; j < peopleData.size(); j++){
            System.out.println(peopleData.get(j).toString());
        }
        System.out.println();
    }

    public static void printHouses(ArrayList<House> houseData){
        System.out.println("House | Residents");
        for(int i = 0; i < houseData.size(); i++){
            System.out.print(houseData.get(i).toString() + " ");
            for (int j = 0; j < houseData.get(i).placeSize; j++){
                System.out.print(houseData.get(i).residents.get(j)
                                                .getName() + " ");
           }
         System.out.println();
        }
        System.out.println();
    }

    public static void printWorkplaces(ArrayList<Workplace> wpData){
        System.out.println("Workplace | Employees");
        for(int i = 0; i < wpData.size(); i++){
            System.out.print(wpData.get(i).toString() + " ");
            for (int j = 0; j < wpData.get(i).placeSize; j++){
                System.out.print(wpData.get(i).residents.get(j).
                                                getName() + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void printHistogram(int[] histogram, int bound){
        // print the histogram
        for (int i = 0; i < bound; i++) {
            System.out.printf( " %2d", i );
            for (int j = 0; j < histogram[i]; j++ ) {
                System.out.print('X');
            }
            System.out.print( '\n' );
        }
        System.out.println();
    }

    public static class Person{
        String name = super.toString();
        House home = null;
        HealthStatus infectionState = HealthStatus.uninfected;

        enum HealthStatus{
            uninfected,
            latent,
            infectious,
            bedridden,
            recovered,
            dead
        }

        public House getHome(){return home;}
        public void setHome(House newHome){home = newHome;}
        public HealthStatus getInfectionState(){return infectionState;}
        public void setInfectionState(HealthStatus newInfectionState){
infectionState = newInfectionState;
        }
        public String getName(){return name;}

        public String toString(){ return (lazyFormat(true, (name + " " +
                home + " " + infectionState)));}
    }

    public static class Employee extends Person {
        Workplace wpLocation;

        public String toString() {
            return (lazyFormat(true, (super.name + " " + super.home + " " +
                    wpLocation + " " + super.infectionState)));
        }
    }

    public static class Place{

        LinkedList<Person> residents = new LinkedList<Person>();
        int placeSize;

        public Place(int hs) {
            placeSize = hs;
        }
    }

    public static class House extends Place{

        public House(int hs) {
            super(hs);
        }
    }

    public static class Workplace extends Place{

        public Workplace(int hs) {
            super(hs);
        }
    }

    public static class Error{

        public static void warn(String message) {
            System.out.println( message );
        }

        public static void fatal(String message) {
            warn( message );
            System.exit( 1 );
        }
    }

}
