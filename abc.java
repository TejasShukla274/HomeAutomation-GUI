import java.util.ArrayList;
public class abc {
    //merging two arrrays
    // putting each number differently
    //putting each letter differently
    public static void main(String[] args) {
//         ArrayList<Integer> al = new ArrayList<>();
//         int n = 123;
//         String ab = String.valueOf(n);
//         String s = "tejas";
//         for(int i = 0 ; i <ab.length();i++){
// al.add(ab.charAt(i)-'0');        }
//     System.out.println(al);
int[] ar = {1,2,3};
int[] arr = {4,5,6};
int[] c = new int[ar.length+arr.length];
int k = 0 ;
for(int i = 0 ; i <ar.length;i++ ){
    c[k++] = ar[i];
}
for(int i = 0 ; i <ar.length;i++ ){
    c[k++] = arr[i];
}
    System.out.print(c.length);

    }
}
