#include <time.h>
#include <sys/types.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

class s_elmnt {
public:
	int cnt[20];
	s_elmnt()  {int i; for (i=0; i< 20; i++) { cnt[i] = 0; }  }
};


class secondary {
public:
	s_elmnt lp[20]; 
};


class SortCounts {
public:
        char year[5], day[3], month[3];
        long midnight, tics[20], numtics;
	char dbname[120];
	char *ticname[100];

        secondary ms[20];
        secondary ts[20];
	s_elmnt  srg[300];
        long numsec, numlp;
	char *lpname[100], *secname[100];

	int	srgmem[9999];
	char	*srgname[500];

	SortCounts(); ~SortCounts();
	void get_dbname(); 
	void get_tics(); 
	void get_srgdata(); 
	void sort_counts(); 
};

