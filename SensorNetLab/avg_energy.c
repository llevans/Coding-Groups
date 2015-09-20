#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char** argv)
{
	char nline[140], tmp[40], *tmp2;
	float time, energy, avg_energy;
	float average[200], ecount[7], tic=1.0;
	int tcount[7], node, i;

	FILE *fp, *ofp;

	for (i=0;i<7;i++)
		ecount[i]=100.00;

	fp = fopen("s802.out", "r");

	while (fgets(nline, 140, fp) != 0)
	{
	     if (nline[0] != 'M' && nline[0] != 'N' && nline[0] != 'D') {
               snprintf(tmp, 12, "%s", &nline[2]); time = atof(tmp);
               tmp[0] = nline[15]; tmp[1]= '\0'; node = atoi(tmp);
	       tmp2 = strstr(nline, "energy");
               snprintf(tmp, 12, "%s", &tmp2[7]); energy = atof(tmp);

	       if (time < tic)
		  ecount[node] = energy;
	       else {
	          avg_energy = 0.0;
		  for (i=0;i<6;i++)
		    avg_enegy+=ecount[i];
	          avgerage[tic] = avg_energy / 7;
	          printf("%d %f\n", tic, average[tic]);
		  tic++;
	       } 
	     }
	   }
	}

	close(fp);

}
