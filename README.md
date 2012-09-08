MoneyBalance
============

An Android-based calculator for tracking and balancing expenses.

Use Case
--------

You're travelling with friends, and as usual there are many bills to pay:
Accomodation, restaurant bills, admittance fees, and so on. Instead of clumsily
splitting each bill as it comes in anybody from the group picks up the whole
check. The payer, the amount, and optionally the split ratio are entered into
the app. At the end of the vacation the app will tell excatly how much each
person paid and consumed. Those who paid too little put the difference into a
pot, those who paid too much take out theirs, and the score is settled.

Features
--------

* Managing multiple calculations in parallel
* Uneven split of expenses
* English and german localization

Warning
-------

This is my first Android project, and I'm pretty sure I'm violating about any
kind of best practice there is. Don't use it as a reference if your getting
started with Android yourself. Just to name a few of its shortcomings:

* I'm not yet using the Android support library. The app will probably not
  work with phones running Android 2.x.
* I'm not using ContentProviders for database access. Should I? I'm not
  sure.
* I'm using floating point arithmetic for monetary calculations (the
  actual expenses are stored as fixed point numbers, though).

If that didn't discourage you: Have fun!

Acknowledgements
----------------

Thanks to Lars Vogel for his brilliant
[Android tutorials][http://www.vogella.com/android.html].
Thanks also to the countless people answering questions on
[Stack Overflow][http://stackoverflow.com], or asking just what I wanted to
know, too).
