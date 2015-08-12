# BaseSpace IGV

[IGV](http://www.broadinstitute.org/igv/) is a fully featured genome browser that allows you to visualize your sequence data in great detail. We’ve modified IGV to display alignment and variant data from BaseSpace (BAM and VCF files). This allows you to perform variant analysis after launching Resequencing or Amplicon workflows in BaseSpace. IGV retains all of its native functions, including loading data from your local computer.

**BaseSpace Data in IGV**

The BaseSpace file browser shows data in BaseSpace that is available for viewing in IGV. The directory structure shown is according to how data is organized in BaseSpace. 

A Project is the highest level directory and it contains one or more AppResults. If an AppResult was the result of analyzing a single sample, then the sample name is appended to the AppResult name. Each AppResult contains zero or more files. 

Alignment (BAM), variant (VCF), BigWig (BW), BED, and BedGraph files are shown in the file browser. Double click a file to load it as an IGV track.

**BaseSpace Data Permissions**

When the IGV app is launched, the user grants the app access to read all data in a BaseSpace Project. This Project is shown in the file browser, and any data from that project may be loaded into IGV. If a user wishes to load data from another Project that hasn't before been launched with the IGV app, the user must return to BaseSpace to launch IGV with the new Project.


**BaseSpace Genomes in IGV**

To view BaseSpace data in IGV, the appropriate genome must be selected for your data in IGV.

For the following BaseSpace genomes, please select the corresponding IGV genome:

Homo sapiens: Human hg19
Mus musculus: Mouse mm9
Saccharomyces cerevisiae: S. cerevisiae (sacCer2)
Arabidopsis thaliana: A. thaliana (TAIR10)


For the following BaseSpace genomes, you will need to manually add the genome to IGV. Download the following compressed file from the Illumina ftp server (copy and paste the link below into your web browser). To uncompress the downloaded file:

On Mac, just double-click your downloaded file
On Linux, on the command line execute: ‘tar –zxvf your_file.tar.gz’
On Windows, use 7-zip (free at www.7-zip.org)

In IGV, select ‘File->Load Genome From File…’. Then select the genome.fa file from your uncompressed download, such as:
Species/Source/Build/Sequence/WholeGenome/genome.fa

PhiX: ftp://igenome:G3nom3s4u@ussd-ftp.illumina.com/PhiX/Illumina/RTA/PhiX_Illumina_RTA.tar.gz
Staphylococcus aureus (strain NCTC 8325): ftp://igenome:G3nom3s4u@ussd-ftp.illumina.com/Staphylococcus_aureus_NCTC_8325/NCBI/2006-02-13/Staphylococcus_aureus_NCTC_8325_NCBI_2006-02-13.tar.gz
E. coli (strain DH10B): ftp://igenome:G3nom3s4u@ussd-ftp.illumina.com/Escherichia_coli_K_12_DH10B/NCBI/2008-03-17/Escherichia_coli_K_12_DH10B_NCBI_2008-03-17.tar.gz
For E. coli, rename the first line in genome.fa from ‘>chr’ to ‘>ecoli’

Other genomes or custom genomes are not available at this time.

**Compiling**

See https://confluence.illumina.com/display/PAT/Basespace+IGV
