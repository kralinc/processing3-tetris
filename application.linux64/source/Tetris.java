import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.sound.SoundFile; 
import ddf.minim.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Tetris extends PApplet {




static final int BOARD_SIZE_Y = 20, BOARD_SIZE_X = 10, TILE_SIZE = 35 ;
static final int BG = (0), RED = (0xffFF0A0A), GREEN = (0xff50FF50), BLUE = (0xff4040FF), 
                             YELLOW = (0xffEEEE22), ORANGE = (0xffFFA010), PURPLE = (0xffCC3399), 
                             MIDORI = (0xff66FFFF), PINK = (0xff880000);

int[][] board;
int[][] colors;
Board theBoard;
Piece thePiece;
GM gm;

SoundFile music;
Minim minim;
AudioSample rotate, move, row, land;

int timer = 400;
int score = 0;


public void setup(){
 
 background(BG);
 //Set up the board and piece
 board = new int[BOARD_SIZE_X][BOARD_SIZE_Y];
 colors = new int[BOARD_SIZE_X][BOARD_SIZE_Y];
 int[][] placeHolderPiece = new int[][]{{4, 0}, {3, 0}, {5, 0}, {4, 1}};
 
 //Set up the sounds and music
 //music = new SoundFile(this, "TypeA.wav");
 //music.loop();
 minim = new Minim(this);
 rotate = minim.loadSample("rotate.wav");
 move = minim.loadSample("move.wav");
 row = minim.loadSample("row.wav");
 land = minim.loadSample("land.wav");
 
 //Create the board and piece
 theBoard = new Board(board, colors);
 thePiece = new Piece(placeHolderPiece, GREEN, theBoard);
 gm = new GM(0);
}

public void draw(){
 background(BG);
 //Do these things if the game is still running
 if (gm.game){
   if (!gm.paused){
     gm.timer = gm.autoDrop(thePiece);
   }
   thePiece.display();
 }
 gm.display();
 theBoard.display();

 
}

public void keyPressed(){
 if (gm.game){
   thePiece.setMove(keyCode, true);
   //Pause/unpause the game when enter is pressed.
   if (keyCode == ENTER){
    gm.paused = !gm.paused;
    gm.timer = millis() + gm.interval;
   }
 //Reset the game if the game has been lost and the player presses enter
 }else{
  if (keyCode == ENTER){
   gm.reset();
  }
 }
}

public void keyReleased(){
 thePiece.setMove(keyCode, false); 
}


class Piece{
  
 int[][] pos;
 int col;
 Board board;
 
 boolean isLeft, isRight, isDown, isRotate;
 
 Piece(int[][] pos_, int col_, Board board_){
  pos = pos_;
  col = col_;
  board = board_;
 }
 
 //Display the piece at its current position
 public void display(){
  fill(col);
  for (int x = 0; x < pos.length; x++){
    int posX = pos[x][0];
    int posY = pos[x][1];
    rect(posX*TILE_SIZE, posY*TILE_SIZE, TILE_SIZE, TILE_SIZE);
  }
 }
 
 //k is the keycode sent to the function. b is the boolean value we're going to set the equivelant boolean to.
 public boolean setMove(int k, boolean b){
   
   //Create a value of 1 or 0 depending on if k is pressed or not.
   switch(k){
    case 'A':
    case LEFT:
      isLeft = b;
      break;
    case 'D':
    case RIGHT:
      isRight = b;
      break;
    case 'S':
    case DOWN:
      isDown = b;
      break;
    case UP:
    case 'W':
    case ' '://Space
      isRotate = b;
      break;
   }
   //t tells the function whether or not to perform a move based on this. It's false when a key is released, rather than pressed.
   if (b){
     move();
   }
   return b;
   
 }
 
 //Move the piece according to input
 public void move(){
   //Drop the piece 1 tile
   if (isDown){
    drop();
   }
   
   //Change the position of the piece horizontally
   if (isLeft || isRight){
     if (ghost(pos, PApplet.parseInt(isRight) - PApplet.parseInt(isLeft), 0)){
       move.trigger();
       pos = shift(pos, PApplet.parseInt(isRight) - PApplet.parseInt(isLeft), 0);
     }
   }
   //Rotate the piece clockwise
   if(isRotate){
    pos = rotatePiece(); 
   }
 }
 
 //Move the entire piece.
 public int[][] shift(int[][] arr, int x, int y){
  for (int i = 0;  i < arr.length; i++){
    arr[i][0] += x;
    arr[i][1] += y;
  }
  return arr;
 }
 
 //If you can, drop the piece 1 tile
 public void drop(){
  if (ghost(pos, 0, 1)){
    pos = shift(pos, 0, 1);
  }else{
    gm.makeNewPiece(this, board);
    return;
  }
 }
 
 //Rotate the piece clockwise.
 public int[][] rotatePiece(){
   int[][] rotPos = convert();
   //Rotates the piece by swapping x and y and reversing the sign of x
   //Then it adds the origin point to the rotation
   for (int x = 1; x < rotPos.length; x++){
    int temp = rotPos[x][0];
    rotPos[x][0] = (rotPos[x][1] * -1) + pos[0][0];
    rotPos[x][1] = temp + pos[0][1];
   }
   
   //Check if the move is valid.
   if (ghost(rotPos, 0, 0)){
     rotate.trigger();
     return rotPos;
   }else{
    return pos; 
   }
 }
 
 //Turn the actual coordinates into relative coordinates, for use with rotation.
 public int[][] convert(){
   int[][]arr = new int[pos.length][2];
   arr[0] = pos[0];
   for (int x = 1; x < pos.length; x++){
     int posX = pos[x][0] - pos[0][0];
     int posY = pos[x][1] - pos[0][1];
     arr[x] = new int[]{posX, posY};
   }
   return arr;
 }
 
 
 //This tests to see if a move is valid by "ghosting" the move ahead of time, so an error isn't made.
 public boolean ghost(int[][] pos, int x, int y){
   
  //Copy the array to the new ghost array, and add the movement with it
  int[][] ghost = new int[pos.length][2];
  for (int i = 0; i < pos.length; i++){
   ghost[i][0] = pos[i][0]+x;
   ghost[i][1] = pos[i][1]+y;
  }
  
  //See if it fails any boundary checks
  //These include out of bounds left and right, below the screen, and inside a piece
  //If any of these fail or it causes an error, don't perform the move.
  for (int i = 0; i < ghost.length; i++){
    try{
      if (ghost[i][0] < 0 || ghost[i][0] >= BOARD_SIZE_X || ghost[i][1] >= BOARD_SIZE_Y || board.board[ghost[i][0]][ghost[i][1]] != 0){
       return false; 
      }
    }catch(ArrayIndexOutOfBoundsException e){
      return false;
    }
  }
  //OK the move if there are no errors.
  return true;
  
 }
 
}

//------------The Board Class
class Board{
 int[][] board;
 int[][] colors;
 
 Board(int[][] board_, int[][] colors_){
   board = board_;
   colors = colors_;
 }
 
 //Display the blocks on the board.
 public void display(){
   //Loop through the board and draw a tile where the board data says there is a piece.
   for (int x = 0; x < board.length; x++){
     for (int y = 0; y < board[x].length; y++){
       if (board[x][y] != 0){
         fill(colors[x][y]);
         rect(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
       }
     }
   }
 }
 
 //Add the current piece to the board.
 public void addPiece(Piece piece){
   for (int x = 0; x < piece.pos.length; x++){
     int xPos = piece.pos[x][0];
     int yPos = piece.pos[x][1];
     board[xPos][yPos] = 1;
     colors[xPos][yPos] = piece.col;
   }
 }
 
 //This function will see if a whole line has been used and clear the line, then collapse the ones above it.
 public void checkRow(){
  int rowsThisTurn = 0;
  //Look through the whole board and see if the row has any empty squares
  for (int x = 0; x < board[0].length; x++){
    boolean fullRow = true;
    
   for (int y = 0; y < board.length; y++){
    if (board[y][x] == 0){
     fullRow = false;
     break;
    }
   }
   
   //If there are no empty squares
   if (fullRow){
     rowsThisTurn++;
     gm.rows++;
     gm.score += BOARD_SIZE_X * (rowsThisTurn * 1.5f);
     row.trigger();
     
     //Clear the line
     for (int y = 0; y < board.length; y++){
      board[y][x] = 0;
      colors[y][x] = (0);
     }
     
     //Move all of the lines above it down 1 tile
     for (int x2 = 0; x2 < board.length; x2++){
      for (int y = x-1; y >=0 ; y--){
        //If the current tile has a blank tile under it, move it down
        if (board[x2][y] == 1 && board[x2][y+1] == 0){
         board[x2][y] = 0;
         int temp = colors[x2][y];
         colors[x2][y] = (0);
         
         board[x2][y+1] = 1;
         colors[x2][y+1] = temp;
        }
        
      }
     }
     
    }
    
   }
 }//End of checkRow()
 
 public void empty(){
   for (int x = 0; x < board.length; x++){
    for (int y = 0; y < board[x].length; y++){
     board[x][y] = 0;
     colors[x][y] = (0);
    }
   }
 }
 
}

class GM{
 int score;
 int rows;
 int level;
 int interval;
 int timer;
 boolean game;
 boolean paused;
 
 int nextPiece;
 int[][] piecePreview;
 int nextPieceCol;
 
 GM(int level_){
   score = 0;
   rows = 0;
   level = level_;
   interval = 600 - (level * 50);
   game = true;
   paused = false;
   
   nextPiece = PApplet.parseInt(random(0,7));
   piecePreview = new int[][]{{4, 1}, {2, 1}, {3, 1}, {5, 1}};
   nextPieceCol = BLUE;
 }
 
 public void display(){
   fill(0xffAAAAAA);
   rect(BOARD_SIZE_X * TILE_SIZE, 0, width, height);
   textSize(width/25);
   fill(255);
   text("Score: " + score + "\nRows: " + rows + "\nLevel: " + level, BOARD_SIZE_X * TILE_SIZE + 25, 25);
   if (!game){
    text("Game over!\nPress enter to\nplay again.", BOARD_SIZE_X * TILE_SIZE + 25, 150);
   }
   if (paused){
     text("Game Paused.\nPress enter to\nresume.", BOARD_SIZE_X * TILE_SIZE + 25, 150);
   }
   
   int previewSize = PApplet.parseInt(TILE_SIZE);
   
   for (int i = 0; i < piecePreview.length; i++){
     fill(nextPieceCol);
     rect(width - 45 - piecePreview[i][0] * previewSize, height - 150 - piecePreview[i][1] * previewSize, previewSize, previewSize);
   }
   
 }
 
 //Automatically drop the piece once the interval is exceeded
public int autoDrop(Piece piece){
  if (millis() > timer){
   piece.drop();
   if (rows > (level + 1) * 15){
    level++;
    interval = 600 - (level * 50);
   }
   return timer + interval;
  }
  return timer;
}

public void lose(){
  game = false;
  //music.stop();
}

//Make a new piece and put it at the top
public void makeNewPiece(Piece piece, Board board){
  //Add the current piece to the board
  board.addPiece(piece);
  //Increase the score and play the landing sound
  gm.score += piece.pos.length;
  land.trigger();
  
  //Set the piece to the one stored from last time
  piece.col = nextPieceCol;
  piece.pos = piecePreview;
  
  int seed = PApplet.parseInt(random(0,7));
  //Create a new random piece and store it
  switch (nextPiece){
  case 0:
   nextPieceCol = BLUE;
   piecePreview = new int[][]{{3, 0}, {2, 0}, {4, 0}, {5, 0}};
   break;
  case 1:
   nextPieceCol = GREEN;
   piecePreview = new int[][]{{4, 0}, {3, 0}, {5, 0}, {4, 1}};
   break;
  case 2:
   nextPieceCol = YELLOW;
   piecePreview = new int[][]{{4,0}, {3, 0}, {5, 0}, {5, 1}};
   break;
  case 3:
   nextPieceCol = ORANGE;
   piecePreview = new int[][]{{4,0}, {3, 0}, {5, 0}, {3, 1}};
   break;
  case 4:
   nextPieceCol = PURPLE;
   piecePreview = new int[][]{{4, 0}, {5, 0}, {4, 1}, {5, 1}};
   break;
  case 5:
   nextPieceCol = RED;
   piecePreview = new int[][]{{4, 0}, {3, 0}, {4, 1}, {5, 1}};
   break;
  default:
   nextPieceCol = MIDORI;
   piecePreview = new int[][]{{4, 0}, {5, 0}, {3, 1}, {4, 1}};
  }
  
  if (board.board[piece.pos[0][0]][piece.pos[0][1]] != 0){
    lose();
  }
  nextPiece = seed;
  
  //See if any rows have been filled
  board.checkRow();
}

public void reset(){
  level = 0;
  interval = 600 - (level * 50);
  rows = 0;
  score = 0;
  //music.loop();
  theBoard.empty();
  timer = millis() + interval;
  game = true;
}

 
}
  public void settings() {  size(650,700); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Tetris" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
