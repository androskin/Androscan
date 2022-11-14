# Androscan

##Application for making inventory

Exchange over LAN or FTP.

###Input file:  
Goods.csv  
code TEXT;name TEXT;article TEXT;additional TEXT;unit TEXT;divisible INTEGER (0,1);barcode TEXT;price REAL;amount REAL (not used now, always 0)  

###Output file:  
_doc_name_.csv  
doc_number TEXT;date TEXT;comments TEXT  
barcode_1 TEXT;amount_1 REAL  
barcode_2 TEXT;amount_2 REAL  
barcode_3 TEXT;amount_3 REAL  
...  
barcode_n TEXT;amount_n REAL  
   
##Samples:  

Goods.csv  
---------------------  
22081;Water;;;l;0;2100000000012;18;0  
22082;Bread;;;kg;0;2100000000029;25.15;0  
22083;Butter;;;kg;0;2100000000036;10.73;0  
---------------------  

AT1-17.10.2022.csv  
---------------------  
AT1;17.10.2022;111  
22090;1  
8710847963469;1  
8710847963469;1  
8710847963469;1  
123;1  
123;1  
---------------------  