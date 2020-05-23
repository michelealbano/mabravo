# mabravo
Simulator for the MABRAVO overlay routing algorithm

# usage

To deploy the system: execute on the command line:
> make release


# execution modes

The first execution mode is the graphical mode.
It is executed by providing 3 parameters on the command line:
- the number of sites;
- the number of points defining the AoI;
- a random seed.

Example:
> java -cp mabravo-1.0.5/mabravo-1.0.5.jar mabravo.Mabravo 100 10 1000

The other execution mode is the batch mode. It allows to perform a series of different simulations of the system, and to extract performance indicators. Five parameters are passed from the command line:
- the number of sites;
- the number of points defining the AoI;
- the number of routing processes to be performed over each network;
- the number of networks to be simulated;
- a random seed.

Example:
> java -cp mabravo-1.0.5/mabravo-1.0.5.jar mabravo.Mabravo 100 10 100 10 100

