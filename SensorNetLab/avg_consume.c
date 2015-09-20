#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char** argv)
{
	char c, fn[20], ofn[20];
	float time, energy, avg_consume;
	float consume[7], ecount[7], tic=1.0;
	int node, i, look, l=0, source=0;

	FILE *fp, *ofp;

	if (argc < 2) exit(0);
	else { strcpy(fn, argv[1]); strcat(fn, ".out");
	       strcpy(ofn, argv[1]);strcat(ofn, ".csm"); }

	if (argc == 3) { if (!strcmp(argv[2],"s")){source++;strcat(ofn,"s");}}

	for (i=0;i<7;i++)
	    ecount[i]=100.00;

	fp = fopen(fn, "r");
	ofp = fopen(ofn, "w");

	while ((c=fgetc(fp)) != EOF)
	{
	     if (c != 'M' && c != 'N' ) {
               c = fgetc(fp);
               fscanf(fp, "%f", &time); 

               c = fgetc(fp);
	       c = fgetc(fp);

	       fscanf(fp, "%d", &node);
	
	       look=1;
	       while(look) {if ((c=fgetc(fp)) == 'e') {if ((c=fgetc(fp)) == 'n') look--;}}
               c = fgetc(fp);
	       c = fgetc(fp);
               c = fgetc(fp);
               c = fgetc(fp);
	       c = fgetc(fp);

               fscanf(fp, "%f", &energy);
	       while ((c=fgetc(fp)) != '\n') {}

	       if (time < tic) {
		  ecount[node] = energy;
	       }
	       else {
		     if (!source)   {
	             avg_consume = 0.0;
		     for (i=0;i<3;i++)  {
		       consume[i] = 100.0 - ecount[i];
		       avg_consume += consume[i];
		     }
	             avg_consume = avg_consume / 3.0;
                     fprintf(ofp, "%3.1f   %f\n", tic, avg_consume); 
	            }
		    else  {
		     consume[3] = 100.0 - ecount[3];
                     fprintf(ofp, "%3.1f   %f\n", tic, consume[3]); 
		    }

		  tic++;
	       } 
	     }
	 }

	close(fp);
	close(ofp);
}
