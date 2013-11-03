MoneyBalance
============

An Android-based calculator for tracking and balancing group expenses.

Use Case
--------

You're travelling with friends, and as usual there are many bills to pay:
Accomodation, restaurant bills, admittance fees, and so on. Instead of clumsily
splitting each bill as it comes in anybody from the group picks up the whole
check. The payer, the amount, and optionally the split ratio are entered into
the app. At the end of the vacation the app will tell excatly how much each
person paid and consumed. Those who paid too little put the difference into a
pot, those who paid too much take out theirs, and the score is settled.

The collected data of a calculation can be written to a CSV file which can be
imported into a spreadsheet application like Excel or LibreOffice Calc. The
following import parameters should be used:

* Character set UTF-8
* Language English (using a point as decimal mark in numbers)
* Fields separated by comma
* Double quotes as text delimiter

Features
--------

* Managing multiple calculations in parallel
* Uneven split of expenses
* CSV export for importing calculations into a spreadsheet application
* English, german, french and spanish localization

Download
--------

Binary builds are maintained by the F-Droid project and can be downloaded
from:

https://f-droid.org/repository/browse/?fdid=ivl.android.moneybalance

Dependencies
------------

The project requires the v7 appcompat library from the Android Support
Libraries. It is included in the Android Support Library, revision 18,
at extras/android/support/v7/appcompat. You may have to adjust the library
reference in the project.properties file to build this project. Follow the
instructions at http://developer.android.com/tools/support-library/setup.html
(section "Adding libraries with resources") for details.

Warning
-------

This is my first Android project, and I'm pretty sure I'm violating about any
kind of best practice there is. Don't use it as a reference if your getting
started with Android yourself. Just to name a few of its shortcomings:

* I'm not using ContentProviders for database access. Should I? I'm not
  sure.
* I'm using floating point arithmetic for monetary calculations (the
  actual expenses are stored as fixed point numbers, though).

If that didn't discourage you: Have fun!

Acknowledgements
----------------

Thanks to Lars Vogel for his brilliant Android tutorials 
(http://www.vogella.com/android.html).
Thanks also to the countless people answering questions on stackoverflow.com,
or asking just what I wanted to know.

The "Coins" icon set is copyright (C) VisualPharm
(http://www.visualpharm.com/finance_icon_set) and distributed under a Creative
Commons Attribution-No Derivative Works 3.0 Unported license.
