<template>
  <div class="container">
    <nav class="navbar navbar-default">
      <div class="container-fluid">
        <div class="navbar-header">
          <a class="navbar-brand">
            User: {{ user && user.name | uppercase }}
          </a>
        </div>
      </div>
    </nav>
    <div class="col-md-2">
      <div v-if="gameStarted">
        <div class="list-group">
          <a class="list-group-item">
            {{ user.name | uppercase }} {{user.points + ' - ' + opponent.points}} {{ opponent.name | uppercase }}
          </a>
          <a href="#" class="list-group-item" @click.prevent="resign()">
            <span class="text-danger">Resign</span>
          </a>
        </div>
      </div>
      <div v-else>
        <div class="list-group">
          <a class="list-group-item disabled">Challenge User</a>
          <a href="#" class="list-group-item" v-for="item in onlineUsers" @click.prevent="offerGame(item)">
            {{ item.name | uppercase }}
          </a>
        </div>
      </div>
    </div>
    <div class="col-md-5">
      <div class="panel panel-info">
        <div class="panel-heading">
          <user-move-type v-if="gameStarted" :user="user"></user-move-type>
          <span v-else>-</span>
        </div>
        <div class="panel-body" id="game">
        </div>
        <div class="panel-footer">
          <who-can-make-move v-if="gameStarted" :user="user" :opponent="opponent"
                             :userCanMove="canMove"></who-can-make-move>
          <span v-else>-</span>
        </div>
      </div>
    </div>
    <div class="col-md-5">
      <div class="panel panel-success">
        <div class="panel-heading">
          Chat
        </div>
        <div class="panel-body">
          <div class="well" style="height: 265px; overflow: auto" ref="chatBlock">
            <div v-for="item in messages">
              <div :class="['alert', item.who == 'me' ? 'alert-warning' : 'alert-info']" role="alert">
                <strong>{{ item.username | uppercase }}:</strong> {{ item.message }}
              </div>
            </div>
          </div>
          <form class="form-inline" onsubmit="return false;">
            <input class="form-control" v-model="message" @keyup.enter='sendMessage' size="30" type="text"
                   placeholder="Type something..."/>
            <button type="button" @click="sendMessage" class="btn" id="send">Send</button>
          </form>
        </div>
        <div class="panel-footer">
          ------
        </div>
      </div>
    </div>
    <sign-in v-if="showSignInModal" @signIn="signIn"></sign-in>
    <accept-offer v-if="showAcceptOfferModal" @accept="acceptOffer"
                  :username="opponent ? opponent.name : ''"></accept-offer>
  </div>
</template>


<script>
  import SignIn from './SignIn'
  import AcceptOffer from './AcceptOffer'
  import UserMoveType from './UserMoveType'
  import WhoCanMakeMove from './WhoCanMakeMove'
  import * as PIXI from 'pixi.js'
  import * as io from 'socket.io-client'

  let app = null;
  let renderer = null;
  let container = null;
  let board = null;

  let MoveType = {
    NONE: 0,
    X: 1,
    O: 2,
    Draw: 3,
  };

  let socket = io('http://127.0.0.1:8080');

  export default {
    data() {
      return {
        width: 302,
        height: 302,
        messages: [],
        message: '',
        user: null,
        opponent: null,
        canMove: false,
        onlineUsers: [],
        showSignInModal: false,
        showAcceptOfferModal: false,
        gameStarted: false
      }
    },

    components: {
      'sign-in': SignIn,
      'accept-offer': AcceptOffer,
      'who-can-make-move': WhoCanMakeMove,
      'user-move-type': UserMoveType
    },

    created() {
      this.notReactiveData = {
        ackServerCallback: null,
        filledCells: null,
        board: null,
      };

      this.registerSocketListeners();

      this.showSignInModal = true;

      this.initializeBoard();
    },

    filters: {
      uppercase: function (v) {
        if (!v)
          return '';
        return v.toUpperCase();
      }
    },

    methods: {
      signIn(username) {
        socket.emit('sign_in', username);

        this.showSignInModal = false;
      },

      offerGame(user) {
        this.opponent = user;
        socket.emit('join_game_offer', user.id);
      },

      acceptOffer(yesNo) {
        if (this.notReactiveData.ackServerCallback) {
          this.notReactiveData.ackServerCallback(yesNo);
          this.notReactiveData.ackServerCallback = null;
        }
        this.showAcceptOfferModal = false;
      },

      makeMove(e) {
        let cellWidth = ~~(this.width / 3);
        let cellHeight = ~~(this.height / 3);

        let i = ~~(e.data.global.y / cellHeight);
        let j = ~~(e.data.global.x / cellWidth);

        if (this.canMove && !this.notReactiveData.filledCells[i][j]) {
          let move = {
            i: i,
            j: j
          };
          socket.emit('make_move', move);
        }
      },

      startGame() {
        this.gameStarted = true;
        this.enableBeforeUnloadEvent(true);
        this.canMove = this.user.type === MoveType.X;

        this.notReactiveData.filledCells = [[0, 0, 0], [0, 0, 0], [0, 0, 0]];

        while (true) {
          let index = container.children.findIndex(child => child.tag === "remove");
          if (index === -1)
            break;
          container.removeChild(container.children[index]);
        }
      },

      resign() {
        this.endGame();
        socket.emit('resign', 'resigned');
      },

      endGame() {
        this.gameStarted = false;
        this.enableBeforeUnloadEvent(false);
        this.canMove = false;
        this.message = '';
        this.messages = [];
        this.notReactiveData.filledCells = null;
      },

      sendMessage() {
        if (!this.gameStarted)
          return;

        if (!this.message)
          return;

        socket.emit("message", this.message);

        this.messages.push({
          'username': this.user.name,
          'message': this.message,
          'who': 'me'
        });

        this.message = '';

        this.scrollTopToBottomChat();
      },

      drawX(i, j) {
        let cellWidth = ~~(this.width / 3);
        let cellHeight = ~~(this.height / 3);

        let x = j * cellHeight;
        let y = i * cellWidth;

        let graphics = new PIXI.Graphics();
        graphics.lineStyle(10, 0x33FF00);
        graphics.moveTo(cellWidth / 2, 0);
        graphics.lineTo(cellWidth / 2, cellHeight);
        graphics.moveTo(0, cellHeight / 2);
        graphics.lineTo(cellWidth, cellHeight / 2);

        let texture = renderer.generateTexture(graphics);

        let X = new PIXI.Sprite(texture);

        X.rotation = Math.PI / 4;

        X.position.set(x + cellHeight / 2, y + cellWidth / 2);
        X.anchor.set(0.5, 0.5);

        X.tag = "remove";

        container.addChild(X);
      },

      drawO(i, j) {
        let cellWidth = ~~(this.width / 3);
        let cellHeight = ~~(this.height / 3);

        let x = j * cellHeight;
        let y = i * cellWidth;

        let graphics = new PIXI.Graphics();
        graphics.lineStyle(6, 0xFFAF0B);
        graphics.beginFill(0xFFAF0B, 0);
        graphics.drawCircle(x, y, 35);
        graphics.endFill();

        let texture = renderer.generateTexture(graphics);

        let O = new PIXI.Sprite(texture);

        O.position.set(x + cellHeight / 2, y + cellWidth / 2);
        O.anchor.set(0.5, 0.5);

        O.tag = "remove";

        container.addChild(O);
      },

      drawBackground() {
        let graphics = new PIXI.Graphics();

        let cellWidth = ~~(this.width / 3);
        let cellHeight = ~~(this.height / 3);

        graphics.lineStyle(1, 0x000000, 1);
        graphics.beginFill(0xFFFFFF, 2);
        for (let i = 0; i < 3; i++) {
          for (let j = 0; j < 3; j++) {
            graphics.drawRect(
              i * cellHeight,      // x
              j * cellWidth,       // y
              cellHeight,// width
              cellWidth// height
            );
          }
        }

        graphics.endFill();

        let texture = renderer.generateTexture(graphics);

        return new PIXI.Sprite(texture);
      },

      enableBeforeUnloadEvent(enable) {
        if (enable) {
          window.onbeforeunload = (e) => {
            e = e || window.event;
            // For IE and Firefox prior to version 4
            if (e) {
              e.returnValue = 'Sure?';
            }
            // For Safari
            return 'Sure?';
          };
        }
        else {
          window.onbeforeunload = null;
        }
      },

      showMessage(str, theme) {
        this.$toasted.show(str, {
          theme: theme || "bubble",
          position: "top-center",
          duration: 5000
        });
      },

      registerSocketListeners() {
        socket.on('connect', () => {
          console.log('socket connected')
        });

        socket.on('set_user', (user, ackServerCallback) => {
          this.user = user;

          this.showMessage("<h4>Hello " + user.name + "! Please, select a user to play.</h4>");

          if (ackServerCallback) {
            ackServerCallback("OK");
          }
        });

        socket.on('online_users', (onlineUsers) => {
          if (!!this.user) {
            this.onlineUsers = onlineUsers.filter(u => !!u).filter(u => u.id !== this.user.id);
          }
        });

        socket.on('join_game_offer', (user, ackServerCallback) => {
          this.opponent = user;

          this.showAcceptOfferModal = true;
          this.notReactiveData.ackServerCallback = ackServerCallback;
        });

        socket.on('join_game_deny', () => {
          this.showMessage("Your opponent denied your offer");
        });

        socket.on('start_game', (data) => {
          this.user.type = data.userType;
          this.user.points = data.userPoints;

          this.opponent.type = data.userType === MoveType.X ? MoveType.O : MoveType.X;
          this.opponent.points = data.opponentPoints;

          this.startGame();
        });

        socket.on('make_move', (move, ackServerCallback) => {
          if (move.type === MoveType.X)
            this.drawX(move.i, move.j);
          else
            this.drawO(move.i, move.j);

          switch (move.isWinner) {
            case MoveType.X:
              this.showMessage("The winner is " + this.user.name + " 'X'");
              this.endGame();
              break;
            case MoveType.O:
              this.showMessage("The winner is " + this.opponent.name + " 'O'");
              this.endGame();
              break;
            case MoveType.Draw:
              this.showMessage("Draw");
              this.endGame();
              break;
            default:
              this.canMove = move.type !== this.user.type;
          }

          if (ackServerCallback) {
            ackServerCallback("ok");
          }
        });

        socket.on('end_game', () => {
          this.endGame();
          this.showMessage("Your opponent has left the game");
        });

        socket.on('message', (message) => {
          this.messages.push({
            'username': this.opponent.name,
            'message': message,
            'who': 'her'
          });
          this.scrollTopToBottomChat();
        });

        socket.on('disconnect', () => {
          this.showMessage('disconnected');
        });
      },

      initializeBoard() {
        app = new PIXI.Application(this.width, this.height, {backgroundColor: 0xFFFFFF});
        this.$nextTick(() => {
          document.getElementById("game").appendChild(app.view)
        });

        renderer = app.renderer;
        container = new PIXI.Container();

        board = this.drawBackground();
        board.interactive = true;
        board.on('pointerup', this.makeMove);

        container.addChild(board);
        app.stage.addChild(container);
      },

      scrollTopToBottomChat() {
        this.$nextTick(() => this.$refs.chatBlock.scrollTop = this.$refs.chatBlock.scrollHeight);
      }
    }
  }
</script>
