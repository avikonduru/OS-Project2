//By Kumaravinash Konduru
//kxk151330

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

class BellHop implements Runnable{	//Implements the BellHop Thread

	//Create variables used in the BellHop thread
    public int bellhopNumber;
    public HotelSim hotel;
    public Thread bellhop;
    
    public BellHop(int bellhopNumber, HotelSim hotel){	//Initialize certain variables for the BellHop instance
    	
        this.bellhopNumber = bellhopNumber;
        this.hotel = hotel;
        
        System.out.println("Bellhop "  + bellhopNumber + " created");	//Print out creation of new BellHop instance
        
        bellhop = new Thread(this);	//Start the thread instance for BellHop
        bellhop.start();
        
    }

    @Override
    public void run(){	//Function allows for the thread to run at the instance of each BellHop instance
    	
        try{
        	
            while(true){
                
            	this.hotel.guestRequestHelp.acquire();	//Wait for Guest to request help with bags
            	
            	this.hotel.lock3.acquire();	//Enter into a critical section
                Guest guest = this.hotel.bagQueue.remove();	//Remove Guest from bag queue
                this.hotel.bellhopUsed[guest.guestNumber] = bellhopNumber;	//Append bellhopNumber to bellhopUsed list for Guests
                this.hotel.lock3.release();	//Exit out of critical section

                System.out.println("Bellhop " + bellhopNumber + " receives bags from guest " + guest.guestNumber);	//Print that the bags have been received by Guests
                this.hotel.receivedBags.release();	//Wait for confirmation that the bags have been received by Guest

                this.hotel.guestInRoom[guest.guestNumber].acquire();	//Wait until Guest is in the room, then proceed
                System.out.println("Bellhop " + bellhopNumber + " delivers bags to guest " + guest.guestNumber);	//Print that bags have been delivered to the Guests
                this.hotel.bellhopDelivered.release();	//Signal that bellhop has delivered bags
                this.hotel.bellhopAvailible.release();	//Signal that bellhop is now available for other Guests
                
            }
            
        }catch(Exception e){
        	
        	e.printStackTrace();
        	
        }
    }
}

class FrontDesk implements Runnable{	//Implements the FrontDesk Thread
    
	//Create variables used in the FrontDesk thread
	HotelSim hotel;
	public static int roomNumberIncrementor = 0;
    public int frontDeskEmployeeNumber;
    public Thread frontdesk;

    public FrontDesk(int frontDeskEmployeeNumber, HotelSim hotel){	//Initialize certain variables for the FrontDesk instance
    	
    	this.hotel = hotel;
        this.frontDeskEmployeeNumber = frontDeskEmployeeNumber;
        
        System.out.println("Employee "  + frontDeskEmployeeNumber + " created");	//Print out creation of new FrontDesk instance
        
        frontdesk = new Thread(this);	//Start the thread instance for FrontDesk
        frontdesk.start();
        
    }

    @Override
    public void run(){	//Function allows for the thread to run at the instance of each FrontDesk instance
    	
        try{
        	
            while(true){
            	
            	this.hotel.guestIsReady.acquire();	//Wait process until a Guest is ready to be serviced

            	this.hotel.lock2.acquire();	//Go into critical section to give unique room to each Guest
                Guest guest = this.hotel.guestLine.remove();	//Delete guest from queue
                roomNumberIncrementor++;	//Increment room number to get next room number
                guest.roomNumber = roomNumberIncrementor;    
                this.hotel.lock2.release();	//Exit critical section

                this.hotel.frontDeskUsed[guest.guestNumber] = frontDeskEmployeeNumber;	//Let each Guest know which Front Desk Employee helped them and know what their assigned room is
                this.hotel.roomProvided.release();	//Signal that the room has been provided to Guest
                System.out.println("Front desk employee " + frontDeskEmployeeNumber + " registers guest " + guest.guestNumber + " and assigns room " + guest.roomNumber);

                this.hotel.taskDone[guest.guestNumber].release();	//Signal that the Front Desk is finished help Guest instance
                this.hotel.frontDeskDone.acquire();
                this.hotel.frontDeskAvailible.release();	//Signal that the front desk can now take in another Guest
                
            }
            
        }catch(Exception e){
        	
        	e.printStackTrace();
        	
        }
        
    }
    
}

class Guest implements Runnable{	//Thread used for all Guests

    //Create variables used in the Guest thread
    public HotelSim hotel;
    public int guestNumber;
    public int numberOfBags;
    public int roomNumber;
    public static int joins = 0;
    public Thread guest;
    Random random = new Random();

    public Guest(int guestNumber, HotelSim hotel){	//Initialize certain variables for the Guest instance
    	
        this.hotel = hotel;
        this.guestNumber = guestNumber;
        numberOfBags  = random.nextInt(6);	//Bag number is a random number from 0-5

        System.out.println("Guest "  + this.guestNumber + " created");	//Print out that thread was created
        
        guest = new Thread(this);	//Start thread instance
        guest.start();
        
    }

    @Override
    public void run(){	//Function allows for the thread to run at the instance of each Guest instance
    	
        try {
        	
            System.out.println("Guest " + guestNumber + " enters the hotel with " + numberOfBags + " bags");	//Print that new Guest instance has started

            this.hotel.lock1.acquire();	//This is a critical section where a mutex lock is acquired
            this.hotel.guestLine.add(this);	//Within the critical section the Guest instance is added to a queue of guests
            this.hotel.lock1.release();

            this.hotel.frontDeskAvailible.acquire();	//Wait for an available employee
            this.hotel.guestIsReady.release();	//Signal to show that the guest is ready to be serviced
            this.hotel.taskDone[guestNumber].acquire();	//Wait for each front desk is available for each Guest instance
            this.hotel.roomProvided.acquire();	//Make sure that a room is able to be provided
            System.out.println("Guest " + guestNumber + " receives room key for room " + roomNumber + " from employee " + this.hotel.frontDeskUsed[guestNumber]);	//Print the Guest has received a specific room from Front Desk

            this.hotel.frontDeskDone.release();	//Signal that the Front Desk job for this Guest instance is done and can go away


            if (numberOfBags > 2){	//This loop is run in Guest has more than 2 bag, which requires a bellhop
            	
            	this.hotel.bellhopAvailible.acquire();	//Wait for a bellhop to be availible to Guest instance
                System.out.println("Guest " + guestNumber + " requests help with bags");	//Print out Guest request for assistance
                this.hotel.bagQueue.add(this);	//Add Guest instance to a queue that needs bag assistance
                this.hotel.guestRequestHelp.release();	//Signal that the Bellhop job for this Guest instance is done and can go away

                this.hotel.receivedBags.acquire();	//Section is used so that Guest can enter into the room that the front desk provided
                System.out.println("Guest " + guestNumber + " enters room " + roomNumber);
                this.hotel.guestInRoom[guestNumber].release();	//Signal that guest is inside assigned room

                this.hotel.bellhopDelivered.acquire();	//This section is used to wait for the bellhop to bring bags before going to room
                System.out.println("Guest " + guestNumber + " receives bags from bellhop " + this.hotel.bellhopUsed[guestNumber]);

            }else{	//Runs section if the amount of bag Guest instance has is 2 or less
            	
            	this.hotel.guestInRoom[guestNumber].release();	//Signal that guest is inside assigned room
                System.out.println("Guest " + guestNumber + " enters room " + roomNumber);
                
            }

            System.out.println("Guest " + guestNumber + " retires for the evening");	//Print out that Guest instance retires
            
        }catch(Exception e){
        	
            e.printStackTrace();
            
        }finally{	//Run section once run section is finished running
            
            try{
            	
            	this.hotel.endedGuests();	//Signal that the Guest instance is done
                System.out.println("Guest " + guestNumber + " joined");
                guest.join();	//Join Guest instance thread
                
            }catch(InterruptedException e){
            	
            	e.printStackTrace();
            	
            }
            
        }
    }
}

public class HotelSim{ 
    
	//Shared constants used used between Threads
    public static final int NUMBER_OF_GUESTS = 25;
    public static final int NUMBER_OF_FRONTDESK_EMPLOYEES = 2;
    public static final int NUMBER_OF_BELLHOPS = 2;
    
    //Shared list and miscellaneous variables used between Threads
    public static Queue<Guest> guestLine = new LinkedList<Guest>();
    public static Queue<Guest> bagQueue = new LinkedList<Guest>();
    public static int frontDeskUsed[] = new int [NUMBER_OF_GUESTS];
    public static int bellhopUsed[] = new int [NUMBER_OF_GUESTS];
    public static int guestEnd = 0;

    //Shared Semaphore variables used between Threads
    public static Semaphore taskDone[] = new Semaphore[NUMBER_OF_GUESTS];
    public static Semaphore guestInRoom[] = new Semaphore[NUMBER_OF_GUESTS];
    public static Semaphore lock1 = new Semaphore(1, true);
    public static Semaphore lock2 = new Semaphore(1, true);
    public static Semaphore lock3 = new Semaphore(1, true);
    public static Semaphore guestIsReady = new Semaphore(0, true);
    public static Semaphore guestRequestHelp = new Semaphore(0, true);
    public static Semaphore frontDeskDone = new Semaphore(0, true);
    public static Semaphore bellhopDelivered = new Semaphore(0, true);
    public static Semaphore roomProvided = new Semaphore(0, true);
    public static Semaphore receivedBags = new Semaphore(0, true);
    public static Semaphore bellhopAvailible = new Semaphore(NUMBER_OF_BELLHOPS, true);
    public static Semaphore frontDeskAvailible = new Semaphore(NUMBER_OF_FRONTDESK_EMPLOYEES, true);

    public HotelSim(){	//Instantiate all of the list variables created that correspond the the number of guests 

        for (int i = 0; i < NUMBER_OF_GUESTS; i++){	//Provide values for variables used for Guests
        	
        	frontDeskUsed[i] = 0;
            bellhopUsed[i] = 0;
        	taskDone[i]  = new Semaphore(0, true);
            guestInRoom[i]    = new Semaphore(0, true);
            
        }
        
    }

    public static void main(String[] args){	//Function used in order to run program
    	
    	System.out.println("Simulation Starts");	//Print out when simulation is begun
    	
        HotelSim hotel = new HotelSim();	//Initialize related variables for each Guest

        for (int i = 0; i < NUMBER_OF_FRONTDESK_EMPLOYEES; i++){	//Create a thread for two Front Desk Employees each
        	
            new FrontDesk(i, hotel);	//Create Thread
            
        }

        for (int i = 0; i < NUMBER_OF_BELLHOPS; i++){	//Create a thread for two Bellhops each
        	
            new BellHop(i, hotel);	//Create Thread
            
        }

        for (int i = 0; i < NUMBER_OF_GUESTS; i++){
        	
            new Guest(i, hotel);	//Create Thread
            
        }

        while(HotelSim.guestEnd < NUMBER_OF_GUESTS){	//While loop is used in order to make sure the program is running while there are guests who are still not in their rooms
        	
            System.out.print("");	//Print blank as a busy wait solution
            
        }

        System.out.println("Simulation Ends");	//Print out when simulation is complete
        System.exit(0);	//End program
        
    }

    public static void endedGuests(){	//Function used in order to increment the amount of Guests that have joined
    	
    	HotelSim.guestEnd++;	//Increment variable by one
    	
    }

}