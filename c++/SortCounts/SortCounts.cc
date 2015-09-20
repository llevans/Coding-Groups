#include <SortCounts.h>

SortCounts::~SortCounts()
{
	exit(0);
}

SortCounts::SortCounts() {
	int i, j, k;
 	numsec = 13;
	numlp = 19;
	
	
        for (i = 0; i < 20; i++) {
	  for (j = 0; j < 20; j++) {
		for (k = 0; k < 20; k++) {
			ms[i].lp[j].cnt[k] = 0; } } }

	for (i=5; i<numsec; i++) { secname[i] = (char *)malloc(10); 
				   sprintf(secname[i],"MS%d", i);}	
	for (i=1; i<numlp; i++) { lpname[i] = (char *)malloc(10); 
				  sprintf(lpname[i],"LP%d", i); }	
	lpname[18] = "Total";
}

void SortCounts::get_tics() {
	struct tm *loc;
        time_t clock;
	long hour, min, sec, curr;
	char *hptr, *sptr, *mptr, *dptr;
	char hstr[3], sstr[3], mstr[3], dstr[4];
        char curtime[40];
	int i, mins;

        curr = time(&clock);
        tzset();
        loc = localtime(&clock);
        strcpy(curtime, asctime(loc));        
	dptr = &curtime[0]; strncpy(dstr, dptr, 3); dstr[3] = '\0';
	hptr = &curtime[11]; strncpy(hstr, hptr, 2); hstr[2] = '\0';
	mptr = &curtime[14]; strncpy(mstr, mptr, 2); mstr[2] = '\0';
	sptr = &curtime[17]; strncpy(sstr, sptr, 2); sstr[2] = '\0';
	hour = atoi(hstr);
	min = atoi(mstr);
	sec = atoi(sstr);

	sprintf(day, "%d", loc->tm_mday);
	sprintf(month, "%d", loc->tm_mon + 1);
	sprintf(year, "%d", loc->tm_year + 1900);
	
	if (strlen(day) < 2) {
	  for (i = 2; i > 0; i--) { day[i] = day[i-1]; }
	  day[0] = '0'; }

	if (strlen(month) < 2) {
	  for (i = 2; i > 0; i--) { month[i] = month[i-1]; }
	  month[0] = '0'; }

	midnight = curr - (hour * 3600) - (min * 60) - sec;

	numtics = 15;
	ticname[0] = (char *)malloc(10); sprintf(ticname[0],"10:00");	
	ticname[1] = (char *)malloc(10); sprintf(ticname[1],"10:15");	
	ticname[2] = (char *)malloc(10); sprintf(ticname[2],"10:30");	
	ticname[3] = (char *)malloc(10); sprintf(ticname[3],"10:45");	
	ticname[4] = (char *)malloc(10); sprintf(ticname[4],"11:00");	
	ticname[5] = (char *)malloc(10); sprintf(ticname[5],"11:15");	
	ticname[6] = (char *)malloc(10); sprintf(ticname[6],"11:30");	
	ticname[7] = (char *)malloc(10); sprintf(ticname[7],"11:45");	
	ticname[8] = (char *)malloc(10); sprintf(ticname[8],"12:00");	
	ticname[9] = (char *)malloc(10); sprintf(ticname[9],"12:15");	
	ticname[10] = (char *)malloc(10); sprintf(ticname[10],"12:30");
        ticname[11] = (char *)malloc(10); sprintf(ticname[11],"12:45");	
	ticname[12] = (char *)malloc(10); sprintf(ticname[12],"1:00");	
	ticname[13] = (char *)malloc(10); sprintf(ticname[13],"1:15");	
	ticname[14] = (char *)malloc(10); sprintf(ticname[14],"1:30");	

	mins = 600;
	for (i = 0; i < numtics; i++) {
	   tics[i] = midnight + (mins * 60);
	   mins = mins + 15; }

}
void SortCounts::get_srgdata() {
	char insrg[12], insid[12], prvsrg[12], snum[5];
	long innum, i=0;
	FILE *fp;

	if ((fp = fopen("srgdata", "r")) == NULL) exit(0);

	while (fscanf(fp, "%s %s %s\n", insrg, insid, snum) != EOF)
	{
	  if (strncmp(insrg, prvsrg, 5)) {
	        i++; 
	        strncpy(prvsrg, insrg, 6);
		srgname[i] = (char *)malloc(12*sizeof(char));
		strcpy(srgname[i], insrg); }

	  innum = atoi(snum);
          srgmem[innum] = i;
       }
       fclose(fp);
}

void SortCounts::get_dbname() {
	char cmd[240], getstr[120], tlogs[100], stime[5];
	char *p;
	FILE *fp;

	sprintf(dbname,"/proj/data/prod_data/%s%s%s",year,month,day);

	sprintf(getstr, "Begin of the sort %s/%s/%s", year, month, day);
	sprintf(tlogs, "/proj/data/trace_logs/GMON*.log");
	sprintf(cmd, "egrep -h \"%s\" %s > sstart", getstr, tlogs);
	system(cmd);

	if ((fp = fopen("sstart", "r")) == NULL) exit(0);
	fread(getstr, sizeof(char), 113, fp);
	fclose(fp);
	getstr[113] = '\0';
	p = &getstr[107];
	strncpy(stime, p, 5); stime[2]=stime[3]; stime[3]=stime[4];
	stime[4]='\0';
 	strcat(dbname, stime);	

//	sprintf(cmd, "/proj/data/sort_tables/admin/dbexp %s/prodata", dbname);
//	system(cmd);
//      sprintf(cmd, "mv prodata_t.txt %s", dbname);
//      system(cmd);           

//	sprintf(cmd, "mv %s/prodata_t.txt %s/prodata_t.sav", dbname, dbname);
//	system(cmd);
//	sprintf(cmd, "egrep -v \"BarCode|99999999999\" %s/prodata_t.sav > %s/prodata_t.txt", dbname, dbname);
//	system(cmd);
	
}

void SortCounts::sort_counts() {
	char pdat[150], inrec[250]="\0", *dat, *p, *p1, c;
	long pdiv, mdiv, tdiv, sid;
	int sec, lp, tlp, i, j, k;
	FILE	*fp;

	sprintf(pdat, "%s/prodata_t.txt", dbname);
	if ((fp = fopen(pdat, "r")) == NULL) exit(0);

	while((c = fgetc(fp)) != -1) {
	   i=0;
	   while ((inrec[i] = fgetc(fp)) != 10) i++;
	   p = strchr(inrec, ','); p++;
           dat = strtok(p, ","); pdiv = atoi(dat);
           for (i=0; i<=strlen(dat); i++) p++;
           dat = strtok(p, ","); mdiv = atoi(dat);
	   for (i=0; i<=strlen(dat); i++) p++;
	   dat = strtok(p, ","); tdiv = atoi(dat); 
	   for (i=0; i<=strlen(dat); i++) p++;

	   for (j=0; j<14; j++) {
	      dat = strtok(p, ","); for (i=0; i<=strlen(dat); i++) p++;
	   }

	   dat = strtok(p, ","); sid = atoi(dat); 
	   for (i=0; i<=strlen(dat); i++) p++;
	   dat = strtok(p, ","); for (i=0; i<=strlen(dat); i++) p++;

	   dat = strtok(p, ","); sec = atoi(dat); 
	   for (i=0; i<=strlen(dat); i++) p++;
	   dat = strtok(p, ","); for (i=0; i<=strlen(dat); i++) p++;
	   dat = strtok(p, ","); lp = atoi(dat); 
	   for (i=0; i<=strlen(dat); i++) p++;
	   dat = strtok(p, ","); for (i=0; i<=strlen(dat); i++) p++;
	   dat = strtok(p, ","); tlp = atoi(dat); 
	   for (i=0; i<=strlen(dat); i++) p++;

	   for (j=0; j<27; j++) {
	      dat = strtok(p, ","); for (i=0; i<=strlen(dat); i++) p++;
	   }

	   for (i=0; i<numtics-2; i++) 
	       if (mdiv < tics[i+1]) { ms[sec].lp[lp].cnt[i]++; 
	       srg[srgmem[sid]].cnt[i]++;
		break;}
	}

	fclose(fp);

	if ((fp = fopen("dcounts", "w")) == NULL) exit(0);

	for (j=5; j<numsec; j++)  {
	    fprintf(fp, "\nDivert counts for %s\n\n", secname[j]);
	    fprintf(fp, "\t");
	    for(i=1; i<numtics; i++) fprintf(fp, "%s\t", ticname[i]); 
            fprintf(fp, "Total\n");
	    for(i=1; i<numlp; i++) { 	
               fprintf(fp, "%s\t", lpname[i]); 
	       for(k=0; k<numtics-1; k++)  { 
	        if (i < (numlp-1))
	        ms[j].lp[numlp-1].cnt[k] = ms[j].lp[numlp-1].cnt[k] + ms[j].lp[i].cnt[k];
		ms[j].lp[i].cnt[numtics-1] = ms[j].lp[i].cnt[numtics-1] + ms[j].lp[i].cnt[k];
	        fprintf(fp, "%d\t", ms[j].lp[i].cnt[k]); }
	        fprintf(fp, "%d\n", ms[j].lp[i].cnt[numtics-1]); 
	   }
      }	
      fclose(fp);

      fp = fopen("scounts", "w");
	for (i=1; i<100; i++) {
	  fprintf(fp, "%s\t", srgname[i]);
	  for (k=0; k<numtics-1; k++) fprintf(fp, "%d  ", srg[i].cnt[k]);
	 fprintf(fp, "\n");
       }	
     fclose(fp);
}
