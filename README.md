## CCID host application

This is a Windows host application for the smartcard product, which can be found here: https://github.com/blueriversys/ccid_firmware
<br>
<br>

## How to build
You'll need:
<br>
JDK 8
<br>
Maven 3.6.3 or higher
<br>
<br>
Once the environment is setup, issue this command to build:
<br>
mvn package
<br>
<br>

## How to use it
This is a command line tool, which interacts with the physical smartcard reader+card combo, whose link is above.
<p>
To get a usage description, issue this command:
<br>
C:\cardmanager>java -jar target\manager-0.0.1-SNAPSHOT.jar -help
<br>
<br>
Which results in this:
<br>
<br>
usage: program <options>
<br>
Examples:
<br>
program -init,<password>
<br>
program -changepass,<newpass>,<curpass>
<br>
program -add,c:\myfolder\file.txt,cardfile
<br>
program -retrieve,c:\myfolder\file.txt,cardfile,<password>
<br>
program -list
<br>
program -terminals
<br>
program -delall,<password>
<br>
<br>
To store a file on the card, issue this:
<br>
C:\cardmanager>java -jar target\manager-0.0.1-SNAPSHOT.jar -add,c:\myfolder\file.txt,cardfile
<br>
where "cardfile" is the name by which the file will be recognized in the card (max 28 characters for this name).
<br>
<br>
Later, to retrieve that file from the card, issue this:
<br>
program -retrieve,c:\myfolder\file.txt,cardfile,<password>
<br>
where "cardfile" is the name you used when you stored the file in the card. This command requires password, which you set when you issued the init comand.

