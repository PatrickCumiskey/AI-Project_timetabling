package com.company;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.Random;


public class Main {

    public static void main(String[] args) throws IOException {
        // write your code here

        // Declare variables for input
        int G, P, S, M, C, D;
        //Call input method , this method validates input for each case
        G = takePosIntInput("Enter Number Of Generations", 0);
        P = takePosIntInput("Enter Population Size", 0);
        S = takePosIntInput("Enter Number Of Students", 0);
        M = takePosIntInput("Enter Total Number Of Modules", 1);
        C = takePosIntInput("Enter Number Of Modules In Course", 0);
        //ensure that modules taken per student is less than total modules available
        while (C > M) {
            C = takePosIntInput("Enter Number Of Modules in course can not be larger than available modules", 1);
        }
        D = M / 2;

        // create file in current directory, if it exists this will erase its contents
        FileWriter f2 = new FileWriter("AI17.txt", false);
        f2.close();

        // declare array for student timetable and exam timetable
        int[][] students_schedule = new int[S][C];
        int[][] time_table = new int[S][D];
        int[][][] all_info = new int[P][2][D + 1];
        int[][][] all_info_sorted = new int[P][2][D + 1];
        int[][][] all_info_sorted2 = new int[P][2][D + 1];
        int[] fitness_counts = new int[P];

        // set low and high for values
        int Low = 0;
        int High = 100;

        int fitness_num = 0;
        // create students timetable
        students_schedule = generateStudentTT(C, M, S, D);
        int[][] all_time_table = new int[S][D];

        // create exam timetable, loop for multiple orderings
        // also check clashes and calculate fitness per ordering
        for (int population = 0; population < P; population++) {
            time_table = generateExamTT(M, D, population);
            for (int slot = 0; slot < 2; slot++) {
                for (int day = 0; day < D; day++) {
                    all_info[population][slot][day] = time_table[slot][day];
                }
            }
            // get fitness for each ordering , append to array
            fitness_num = check_clashes(students_schedule, time_table, S, C, D, population);
            all_info[population][1][D] = fitness_num;

            fitness_counts[population] = fitness_num;
        }
        all_info_sorted2 = all_info;
        // Repeat until G = i
        for (int gen_index = 0; gen_index < G; gen_index++) {

            all_info_sorted = all_info_sorted2;
            all_info_sorted2 = new int[P][2][D + 1];

            // Sort the population
            all_info_sorted = selection_process(all_info_sorted, D);
            all_info_sorted = selection_process(all_info_sorted, D);

            System.out.println("Best Ordering of: " + gen_index + " generation " + (Arrays.deepToString(all_info_sorted[0])) + "Fitness: " + all_info_sorted[0][1][D] );

            int first_third = all_info_sorted.length / 3;

            int[][][] s1 = new int[P / 3][2][D + 1];

            // get the first third , s1, and repalce the 3rd third , s3 , with it
            s1 = get_s1(all_info_sorted, first_third, s1);
            all_info_sorted = replace_s3(all_info_sorted, first_third, s1);

            // for each ordering calculate
            for (int pop_index = 0; pop_index < P; pop_index++) {

                // create a radnom number to decide what to do
                Random r = new Random();
                int Result = r.nextInt(High - Low) + Low;
                // reproduction has biggest chance
                if (Result <= 80) {
//                    //reproduction
                    all_info_sorted2[pop_index] = all_info_sorted[pop_index];
                }
                // next is crossover
                if (Result > 80 && Result <= 95) {
                    int[][][] mut_arr = new int[2][2][D + 1];
                    int[][][] mut_arr2 = new int[2][2][D + 1];
                    if (pop_index != all_info_sorted.length - 1) {
                        mut_arr = get_mut(all_info_sorted, first_third, mut_arr, pop_index);
                        mut_arr = crossover(mut_arr, first_third, mut_arr2, D, students_schedule, C);


                        for (int add_mut = 0; add_mut < 2; add_mut++) {
                            all_info_sorted2[pop_index] = mut_arr[add_mut];
                        }
                    } else {
                        all_info_sorted2[pop_index] = all_info_sorted[pop_index];
                        //reproduction
                        continue;
                    }
                }
                //lastly is mutation
                if (Result > 95) {
                    int[][][] mut_arr3 = new int[1][2][D + 1];
                    mut_arr3[0] = all_info_sorted[pop_index];
                    mut_arr3 = mutation(mut_arr3, D, students_schedule, C);
                    all_info_sorted2[pop_index] = mut_arr3[0];

                }


            }
            // get fitness of all orders after a GA has been applied
            for (int population = 0; population < P; population++){
                time_table = all_info_sorted2[population];
                fitness_num = check_clashes(students_schedule, time_table, S, C, D, population);
                all_info_sorted2[population][1][D] = fitness_num;
            }

        }
        all_info_sorted = selection_process(all_info_sorted2, D);
        System.out.println("Best Ordering of: " + G + " generation " + (Arrays.deepToString(all_info_sorted[0])) + "Fitness: " + all_info_sorted[0][1][D] );
    }

    // mutation
    public static int[][][] mutation(int[][][] mut_arr, int D, int[][] students_schedule, int C) {
        // select two random numebrs
        int range = D - 2;
        Random random = new Random();
        int chrom_1 = random.nextInt(range + 1) + 1;
        int chrom_2 = random.nextInt(range + 1) + 1;

        int tmp1 = 0;
        int tmp2 = 0;
        // swap the values at the two random numbers index
        for (int i = 0; i < mut_arr.length; i++) {
            tmp1 = mut_arr[i][0][chrom_1];
            tmp2 = mut_arr[i][1][chrom_2];
            mut_arr[i][0][chrom_1] = tmp2;
            mut_arr[i][1][chrom_2] = tmp1;
        }

        return mut_arr;


    }

    // crossover
    public static int[][][] crossover(int[][][] all_info_sorted, int first_third, int[][][] mut_arr2, int D, int[][] students_schedule, int C) throws IOException {
        // set high and low for cutting point range

        int low_range = 2;
        int high_range = (D * 2) - 2;
        // get random position
        Random random = new Random();
        int cp = random.nextInt(high_range - low_range + 1) + low_range;

        //get orders from the two passed in
        int[][] ord1 = all_info_sorted[0];
        int[][] ord2 = all_info_sorted[1];

        // create 1d holders for these orders
        int[] oned_ord1 = new int[D * 2];
        int[] oned_ord2 = new int[D * 2];
        int half_length = D;
        int index = 0;
        for (int kk = 0; kk < 2; kk++) {
            for (int gg = 0; gg < half_length; gg++) {
                oned_ord1[index] = ord1[kk][gg];
                index += 1;
            }
        }
        index = 0;
        for (int kk = 0; kk < 2; kk++) {
            for (int gg = 0; gg < half_length; gg++) {
                oned_ord2[index] = ord2[kk][gg];
                index += 1;
            }
        }

        // Slice the orders at the cutting point and swap past the cutting point
        int slice_size = oned_ord1.length - cp;
        int[] ord1_slice = new int[slice_size];
        int[] ord2_slice = new int[slice_size];

        for (int ii = 0; ii < slice_size; ii++) {
            ord1_slice[ii] = oned_ord1[ii + cp];

        }
        for (int ii = 0; ii < slice_size; ii++) {
            ord2_slice[ii] = oned_ord2[ii + cp];

        }

        for (int i = 0; i < slice_size; i++) {
            oned_ord1[i + cp] = ord2_slice[i];
        }

        for (int i = 0; i < slice_size; i++) {
            oned_ord2[i + cp] = ord1_slice[i];
        }

        // create list of all modules to be examined, if there are duplicates add value form this list that is
        // not already in the exam ordering as it should be
        ArrayList<Integer> modules_list = new ArrayList<Integer>();
        modules_list = getAllMods(C, students_schedule);
        int[] ord1_done = crossover_swap(modules_list, oned_ord1);
        int[][] ord1_done_2d = new int[2][D + 1];
        for (int w = 0; w < D; w++) {
            ord1_done_2d[0][w] = ord1_done[w];
            ord1_done_2d[1][w] = ord1_done[w + D];
        }

        modules_list = getAllMods(C, students_schedule);
        int[] ord2_done = crossover_swap(modules_list, oned_ord2);
        int[][] ord2_done_2d = new int[2][D + 1];
        for (int w = 0; w < D; w++) {

            ord2_done_2d[0][w] = ord2_done[w];
            ord2_done_2d[1][w] = ord2_done[w + D];
        }
        mut_arr2[0] = ord1_done_2d;
        mut_arr2[1] = ord2_done_2d;


        return mut_arr2;
    }

    // crossover swap called from crossover
    public static int[] crossover_swap(ArrayList<Integer> modules_list, int[] to_order) throws IOException {
        // creates list of unused modules
        for (int i = 0; i < to_order.length; i++) {
            if (modules_list.contains(to_order[i])) {
                modules_list.remove(new Integer(to_order[i]));
            }
        }
        // create list of all modules to be examined, if there are duplicates add value form this list that is
        // not already in the exam ordering as it should be
        int to_check = 0;
        for (int i = 0; i < to_order.length; i++) {
            to_check = to_order[i];
            for (int ii = 1; ii < to_order.length - 1; ii++) {
                if (to_check == to_order[ii] && i != ii && modules_list.size() != 0 ) {
                    to_order[ii] = modules_list.get(0);
                    modules_list.remove(0);
                    i = 0;
                }
            }
        }

        return to_order;
    }
    // Gets all mods used by students
    public static ArrayList<Integer> getAllMods(int C, int[][] students_schedule) {
        ArrayList<Integer> modules_list = new ArrayList<Integer>();
        for (int i = 0; i < students_schedule.length; i++) {
            for (int ii = 0; ii < C; ii++) {
                if (!modules_list.contains(students_schedule[i][ii]))
                    modules_list.add(students_schedule[i][ii]);
            }
        }

        return modules_list;
    }

    // replaces the trid section s3 with the best section s1
    public static int[][][] replace_s3(int[][][] all_info_sorted, int first_third, int[][][] s1) throws IOException {
        int third_third = first_third * 2;
        for (int ww = 0; ww < first_third; ww++) {
            all_info_sorted[third_third + ww][0] = s1[ww][0];
            all_info_sorted[third_third + ww][1] = s1[ww][1];
        }
        return all_info_sorted;


    }
    // gets the best section s1
    public static int[][][] get_s1(int[][][] all_info_sorted, int first_third, int[][][] s1) throws IOException {
        for (int ww = 0; ww < first_third; ww++) {
            s1[ww][0] = all_info_sorted[ww][0];
            s1[ww][1] = all_info_sorted[ww][1];
        }
        return s1;
    }
    // gets two orderings to be used in crossover
    public static int[][][] get_mut(int[][][] all_info_sorted, int first_third, int[][][] mut_arr, int pop_index) throws IOException {
        for (int ww = 0; ww < 2; ww++) {
            mut_arr[ww][0] = all_info_sorted[pop_index + ww][0];
            mut_arr[ww][1] = all_info_sorted[pop_index + ww][1];
        }
        return mut_arr;
    }
    // Orders the selection from best to worst
    public static int[][][] selection_process(int[][][] all_info, int D) throws IOException {
        for (int w = 0; w < all_info.length - 1; w++) {
            // if the fitness function is lower moves infront of current ordering
            if (all_info[w][1][D] > all_info[w + 1][1][D]) {
                // Creates temp array for ordering to be swapped
                int[] tmp = all_info[w][0];
                int[] tmp2 = all_info[w][1];
                // Swaps orderings in array to put best at top
                all_info[w][0] = all_info[w + 1][0];
                all_info[w][1] = all_info[w + 1][1];
                all_info[w + 1][0] = tmp;
                all_info[w + 1][1] = tmp2;
                w = 0;
            }

        }
        return all_info;

    }
    // Calculates fitness function
    public static int check_clashes(int[][] students_schedule, int[][] time_table, int students, int mods_taken, int days, int order) throws IOException {


        // initialise ArraryLists for student timetable and exam
        ArrayList<Integer> studentCopy = new ArrayList<Integer>();
        ArrayList<Integer> tt_copy = new ArrayList<Integer>();

        // add current timetable to arraylist
        for (int h = 0; h < days; h++) {
            tt_copy.clear();
            tt_copy.add(time_table[0][h]);
            tt_copy.add(time_table[1][h]);


        }

        // set match per stduent and fitness to 0
        int daily_match = 0;
        int fitness = 0;
        // for each student loop
        for (int k = 1; k <= students; k++) {
            // clear the arraylist
            studentCopy.clear();
            daily_match = 0;
            // add current student to the arraylist
            for (int j = 0; j < mods_taken; j++) {
                studentCopy.add(students_schedule[k - 1][j]);
            }
            // for each day run through loop
            for (int h = 0; h < days; h++) {
                // set daily match to 0 , clear the timetable and than add todays exams to it
                daily_match = 0;
                tt_copy.clear();
                tt_copy.add(time_table[0][h]);
                tt_copy.add(time_table[1][h]);
                // check if current day of exams is in the student timetable
                for (int g = 0; g < 2; g++) {
                    if (studentCopy.contains(tt_copy.get(g))) {
                        daily_match += 1;
                    }
                }
                // if both exams are in the student timetable add number of clashes to the fitness function
                if (daily_match >= 2) {
                    fitness += daily_match - 1;
                }
            }
        }

        return fitness;


    }

    // generates exam timetable
    public static int[][] generateExamTT(int maxMods, int days, int order) {
        // create arraylist
        ArrayList<Integer> listCopy;
        // add all possible modules to arraylist
        listCopy = getArrayList(maxMods);
        // shuffle arraylist
        Collections.shuffle(listCopy);

        int[][] tt = new int[2][days];
        int number = 0;
        // add modules to timetables, done sequentially yo ensure no duplicates
        for (int slot = 0; slot < 2; slot++) {
            for (int day = 0; day < days; day++) {

                tt[slot][day] = listCopy.get(number);
                number += 1;
            }

        }
        return tt;

    }

    // create students modules taken
    public static int[][] generateStudentTT(int modsToBeTaken, int maxMods, int totalStudents, int days) {

        //set size of student timetables
        int[][] students_schedule = new int[totalStudents][modsToBeTaken];


        Random rand = new Random();
        int modIndex = 0;
        ArrayList<Integer> listCopy;

        // generate students modules
        for (int k = 0; k < totalStudents; k++) {
            listCopy = getArrayList(maxMods);
            modIndex = -1;
            while (listCopy.size() > 0 && modsToBeTaken - 1 > modIndex) {
                modIndex += 1;
                int index = rand.nextInt(listCopy.size());
                students_schedule[k][modIndex] = listCopy.get(index);
                listCopy.remove(index);
            }
        }

        // return all students modules
        return students_schedule;

    }

    public static ArrayList<Integer> getArrayList(int maxMods) {
        ArrayList<Integer> list = new ArrayList<Integer>(maxMods);
        for (int i = 1; i <= maxMods; i++) {
            list.add(i);
        }
        return list;

    }

    // take in input from user ensuring it is a positive integer
    public static int takePosIntInput(String output, int evenNeeded) {

        Scanner input = new Scanner(System.in);
        int input_number = -1;
        System.out.println(output);
        boolean is_pos = true;
        // check if number needes to be even
        if (evenNeeded == 0) {
            while (is_pos) {
                // read in number and check if positive if not inform user of error
                try {
                    input_number = Integer.valueOf(input.nextLine());
                    if (input_number <= 0) {
                        System.out.println("Input Must Be A Positive Integer ");
                        System.out.println(output);
                    } else {
                        is_pos = false;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Input Must Be A Positive Integer  ");
                    System.out.println(output);
                }
            }
        } else {
            while (is_pos) {
                try {
                    input_number = Integer.valueOf(input.nextLine());
                    if (input_number <= 0 || input_number % 2 != 0) {
                        System.out.println("Input Must Be A Positive Even Integer ");
                        System.out.println(output);
                    } else {
                        is_pos = false;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Input Must Be A Positive Integer  ");
                    System.out.println(output);
                }
            }

        }


        return input_number;


    }


}
