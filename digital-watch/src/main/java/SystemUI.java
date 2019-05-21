import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;


/* 생각해볼문제

모드를 바꿀때마다 각 모드에서 가지고 있던 시간정보를 가져와야할듯
ex) timer모드로 바꿀때 timer에 설정된 시간값을 가져옴

현재 리턴값을 저장시키는 변수가 존재

전제조건들에서 어떤 모드의 수정 단계여야한다... << 이런 전제조건 필요
 */

public class SystemUI extends JFrame {
    //UI 변수
    private JPanel mainPanel;
    private JButton btnAdjust;
    private JButton btnMode;
    private JButton btnReset;
    private JButton btnStart;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel centerPanel;


    private ModeSelector modeSelector;
    private Alarm[] alarmList;
    private Tide[] tideList;
    private Timer timer;
    private TimeKeeping timekeeping;
    private Alarm alarm;
    private Stopwatch stopwatch;
    private Tide tide;
    private Moonphase moonphase;
    private int userInput; //int값인지 불확실
    private String currentMode; //원래 int값인데 String으로 일단 수정
    private boolean buzzByAlarm =false; //알람 부저 스테이트

    /*
    새롭게 필요해 보이는 필드들
     */
//    private String[] selectedModes; //원래 String[]인데 arraylist로 수정
    private ArrayList<String> selectedModes = new ArrayList<>();     //modeselector로 넘겨줄선택된 모드들을 나타냄, 그러니까 현재 선택된 모드들
    private TimeDB timeDB; //System이 TimeDB에서 값을 받아오는게 존재
    private boolean timekeepingAdjustState=false; //timekeeping에서 adjust버튼을 누를경우 state가 true로 바뀌어 시간을 조정중임을 알림
    private boolean timerAdjustState=false; //timer에서 adjust버튼을 누를경우 state가 true로 바뀌면서 timer조정가능
    private int timerRunState=0; //시퀀스다이어그램상에서 int이길래 일단 int로 설정 근데 boolean이 더 맞는것같음
    private int timerZeroState=0; //startTimer에서 등장하는 변수 << boolean이여야 할것같은데 일단 int
    private boolean alaramAdjustState=false; //alarm에서 adjust버튼을 누를경우 state가 true로 바뀌면서 alarm조정가능
    private boolean alarmCanAddState=false; //alarm에 alarm을 더 추가시킬 수 있을경우
    private boolean stopwatchAdjustState = false; //stopwatch를 조정중일때
    private int stopwatchRunState=0; //시퀀스다이어그램상에서 int이길래 일단 int로 설정 근데 boolean이 더 맞는것같음
    private int year;
    private int month;
    private int day;
    private String dayOfWeek; //이거는 요일을 계산해주는 알고리즘을 만들어서 알아서 출력시키는게 좋을것같다는 생각
    private int hour;
    private int minute;
    private int second;
    private int cursorState; //현재 커서가 어디 위치인지 나타내주는 커서스테이트
    private HashMap<Integer, Integer> monthMap= new HashMap<>(); //각 월에 맞는 day를 매핑시켜준 hashmap << 근데 윤달을 계산하면 달라질수도있음 timekeeping에 들어가야하는게 아닌가 싶긴한데....


    public static void main(String[] args){
        SystemUI systemUI = new SystemUI();
    }


    public SystemUI() {
        //각월에 맞는 day를 초기화
        //이 값을 TimeDB로 부터 가져오는것으로 바꿈 << 이 값을 대체 어디서 가져와야할까? 일단은 timekeeping이랑  관련된곳
//        monthMap.put(1, 31);
//        monthMap.put(2, 29);
//        monthMap.put(3, 31);
//        monthMap.put(4, 30);
//        monthMap.put(5, 31);
//        monthMap.put(6, 30);
//        monthMap.put(7, 31);
//        monthMap.put(8, 31);
//        monthMap.put(9, 30);
//        monthMap.put(10, 31);
//        monthMap.put(11, 30);
//        monthMap.put(12, 31);

        setContentPane(mainPanel);

        modeSelector=new ModeSelector(); //초기모드 설정을 모드셀렉터에서 진행함

        btnAdjust.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentMode.equals("TimeKeeping")){
                    //adjust 페이즈가 아닐경우 adjusttime으로 진행
                    if(!timekeepingAdjustState) {
                        //currnetMode가 timekeeping일경우 adjust버튼을 누른다면
                        reqAdjustTime(); //으로 진행
                    }
                    //adjust페이즈 일경우 adjustbutton을 누르면 adjust페이즈를 종료
                    else{
                        endAdjustTime();
                    }
                }
                else if(currentMode.equals("Timer")){
                    //adjust페이즈가 아닐경우 adjust페이즈로 진입
                    if(!timerAdjustState){
                        reqSetTimer();
                    }
                    //adjust페이즈에서 빠져나올때
                    else{
                        endSetTimer();
                    }
                }
                else if(currentMode.equals("Alarm")){
                    //알람모드에서 알람이 울리고 있는 경우 stopAlarm이 가능
                    //알람모드의 어떤 상태이던지 간에 stopAlarm이 먼저임
                    //즉 알람을 설정하는 상태여도 알람이 울리면 어떤 버튼을 누르던지 알람을 끔
                    if(buzzByAlarm){
                        reqStopAlarm();
                    }
                    //alarm모드에서 알람을 조정중이지 않은 경우
                    else if(!alaramAdjustState && !buzzByAlarm) {
                        //adjsut버튼을 누를경우 addAlarm실행
                        reqAddAlarm();
                    }
                    //alamr모드에서 알람을 조정중인데 빠져나오려고 할 경우
                    else if(alaramAdjustState && !buzzByAlarm){
                        endAddAlarm();
                    }
                }
                else if(currentMode.equals("Stopwatch")){
                    //stopwatch가 동작중이고 현재 조정가능한 상태가 아닐때 adjust버튼을 누르면 레코드 가능
                    if(stopwatchRunState==1 && !stopwatchAdjustState){
                        reqRecordStopwatch();
                    }
                }
            }
        });
        btnMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentMode.equals("TimeKeeping")){
                    //TimeKeeping모드이고 adjust time일때 mode버튼을 누르면 cursor가 바뀜
                    if(timekeepingAdjustState) {
                        //changeCurosr 실행시 커서의 위치를 변경시켜주는것도 필요
                        changeCursor();
                    }
                    //timekeeping모드이고 adjusttime이 아닐때 mode버튼 누르면 다음 모드로 감
                    else{
                        //(대충 다음 모드로 간다는 코드)
                    }
                }
                else if(currentMode.equals("Timer")){
                    //Timer모드이고 timer를 adjust중일때 mode버튼을 누르면 cursor가 바뀜
                    if(timerAdjustState) {
                        //changeCurosr 실행시 커서의 위치를 변경시켜주는것도 필요
                        changeCursor();
                    }
                    //timer모드이고 settimer모드가 아닐때 mode버튼을 누르면 다음모드로 감
                    else{
                        //(대충 다음 모드로 간다는 코드)
                    }
                }
                else if(currentMode.equals("Alarm")){
                    //알람모드에서 알람이 울리고 있는 경우 stopAlarm이 가능
                    //알람모드의 어떤 상태이던지 간에 stopAlarm이 먼저임
                    //즉 알람을 설정하는 상태여도 알람이 울리면 어떤 버튼을 누르던지 알람을 끔
                    if(buzzByAlarm){
                        reqStopAlarm();
                    }
                    //alarm모드이고 alarm이 설정가능상태일경우 mode버튼을 누르면 cursor가 바뀜
                    if(alaramAdjustState && !buzzByAlarm){
                        changeCursor();
                    }
                    //alarm모드이고 alarm설정상태가 아닐때 mode버튼을 누르면 다음 모드로
                    else if(!alaramAdjustState && !buzzByAlarm) {
                        //(대충 다음 모드로 간다는 코드)
                    }
                }
                else if(currentMode.equals("Stopwatch")){
                    //stopwatch모드이고 작동중이지 않을때 mode버튼을 누르면  cursor가 바뀜 << 이 시나리오가 정의되어 있지 않은듯
                    if(stopwatchAdjustState && stopwatchRunState==0){
                        changeCursor();
                    }
                    //stopwatch모드이고 adjust중이 아닐고 작동중이 아닐때 모드버튼을 누르면 다음 모드로 감
                    else if(!stopwatchAdjustState && stopwatchRunState==0){
                        //(대충 다음 모드로 간다는 코드)
                    }
                }
                else if(currentMode.equals("Tide")){
                    //tide모드일때 mode버튼을 누르면 다음 mode로 넘어감
                    //(대충 다음 모드로 간다는 코드)
                }
                else{
                    //moonphase모드일때 mode버튼을 누르면 다음 mode로 넘어감
                    //(대충 다음 모드로 간다는 코드)
                }
            }
        });
        btnReset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentMode.equals("TimeKeeping")){
                    //뭔가 들어갈게 있겠지
                }
                else if(currentMode.equals("Timer")){
                    /*
                    일단보류

                    //Timer모드이긴한데 현재 타이머가 돌아가는중일경우 reset버튼을 누르면 일단 타이머를 멈춤
                    //전제조건에 Tiemr의 시간이 00:00:00이어야 한다가 있음 << 이거는 추가 안함
                    if(timerRunState==1){
                        reqPauseTimer();
                    }
                    //timer모드이고 현재타이머의 스테이트가 1 이 아니면(0이면: a.k.a timer가 정지해있는경우) reset가능
                    else{
                        reqResetTimer();
                    }
                    */
                    //timer가 adjust단계일경우 reset버튼을 누른다면? << 이 경우는 아직 모르는듯?
                    if(timerAdjustState){
                        //뭔가 들어갈게 있겠지
                    }
                    //현재 timer가 zero이고 adjust단계가 아닐경우
                    if(timerZeroState==0 && !timerAdjustState){
                        reqResetTimer();
                    }
                    //zero가 아니고 adjust단계도 아닐경우
                    else if(timerZeroState==1 && !timerAdjustState){
                        reqPauseTimer();
                    }
                }
                else if(currentMode.equals("Alarm")){
                    //알람모드에서 알람이 울리고 있는 경우 stopAlarm이 가능
                    //알람모드의 어떤 상태이던지 간에 stopAlarm이 먼저임
                    //즉 알람을 설정하는 상태여도 알람이 울리면 어떤 버튼을 누르던지 알람을 끔
                    if(buzzByAlarm){
                        reqStopAlarm();
                    }
                    //alarm모드인데 현재 알람 조정상태가 아닌경우 reset버튼을 누르면 설정된 알람을 제거
                    else if(!alaramAdjustState && !buzzByAlarm){
                        reqDeleteAlarm();
                    }
            /*
            현재 바로 아래의 else부분의 usecase가 정의되어 있지 않음
             */
                    //alarm모드인데 현재 알람 조정상태일경우 reset버튼을 누르면 현재 설정하려고하는 알람을 00:00:00으로 초기화???
                    else if(alaramAdjustState && !buzzByAlarm){
                        //(대충 알람을 시간을 0으로 초기화한다는 내용)
                    }
                }
                else if(currentMode.equals("Stopwatch")){
                    //reset버튼을 누를경우 stopwatch가 동작중일때는 일단 일시정지
                    //동작중이지 않을때는 바로 시간 초기화
                }
            }
        });
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TimeKeeping모드이고 adjust모드일때 현재 커서위치의 값을 증가시킴
                if(currentMode.equals("TimeKeeping") && timekeepingAdjustState){
                    //현재 커서의 위치가 어디인지를 알수있는 방법 필요
                    increaseTime();
                }
                else if(currentMode.equals("Timer")){
                    //Timer모드이고 adjust모드일때 start버튼을 누르면 현재 커서 위치의 값 증가
                    if(timerAdjustState && timerRunState==0) {
                        //현재 커서의 위치가 어디인지를 알수있는 방법 필요
                        increaseTimerTime();
                    }
            /* 중요?
            전제 조건 = Timer상태이고 adjust모드가 아니여야 timer의 start가 가능하다
             */
                    //Timer모드이고 adjust모드가 아닐때 start버튼을 누르면 타이머가 동작
                    //조건이 하나 더필요 <<Timer의 시간이 00:00:00이 아니어야 한다
                    else if(!timerAdjustState && timerRunState==0){ //tiemerAdjustState가 항상 true라는 경고문, 일단 참고
                        reqStartTimer();
                    }
                    //Timer모드이고 adjust모드가 아니고 Timer가 동작중일때 start버튼을 누르면 타이머가 멈춤
                    else if(!timerAdjustState && timerRunState==1){
                        reqPauseTimer();
                    }
                }
                else if(currentMode.equals("Alarm")){
                    //알람모드에서 알람이 울리고 있는 경우 stopAlarm이 가능
                    //알람모드의 어떤 상태이던지 간에 stopAlarm이 먼저임
                    //즉 알람을 설정하는 상태여도 알람이 울리면 어떤 버튼을 누르던지 알람을 끔
                    if(buzzByAlarm){
                        reqStopAlarm();
                    }
                    //alarm모드이고 alarm 설정상태가 아닐경우 start버튼을 누르면 다음알람을 출력
                    else if(!alaramAdjustState && !buzzByAlarm){
                        reqNextAlarm();
                    }
                    //alarm상태이고 alarm설정상태일경우 start버튼을 누르면 설정하는 알람의 현재 커서의 위치값 증가
                    else if(alaramAdjustState && !buzzByAlarm){
                        increaseAlarmTime();
                    }
                }
                else if(currentMode.equals("Stopwatch")){
                    //stopwatch가 조정가능상태가 아닐경우 start가능
                    if(!stopwatchAdjustState && stopwatchRunState==0){
                        reqStartStopwatch();
                    }
                    //stopwatch가 동작중이고 시간조정모드가 아닐경우 start버튼을 누르면 일시정지
                    else if(stopwatchRunState==1 && !stopwatchAdjustState){
                        reqPauseStopwatch();
                    }
                }
                else if(currentMode.equals("Tide")){
                    //Tide모드이고 start버튼을 누르면 다음 tide를 표시
                    reqNextTide();
                }
            }
        });

        setVisible(true);
    }

    //timeDB로부터 가져온 시간을 1초마다 업데이트해줌
    private void showTime(){
        //timeDB.getThread() << 이렇게 쓰는게 맞나?

        //showTime이 gui에 어떤식으로 적용되나 슈도코드
        //textViewYear.setText(year);
        //textViewMonth.setText(month);
        //textViewDay.setText(day);
        //textViewHour.setText(hour);
        //textViewMinute.s etText(minute);
        //textViewSecond.setText(second);
    }

    //현재 커서 위치에 있는 값을 증가시킴
    //사용하는 모드들 = TimeKeeping
    private void increaseTime(){
        //현재 커서에 있는 숫자를 일단 증가시키고 제어문에서 비교

        //현재커서위치의 값
        //curState=0=year   cursorState=1=month     cursorState=2=day
        //cursorState=3=hour    cursorState=4=minute    cursorState=5=second 단, second는 무조건 0으로 초기화
        //dayOfWeek=요일은 유저가 설정 불가능 << 알아서 계산해주는게 좋을듯
        //second는 설정불가능하게 하자고 합의 했던 기억
        //현재 adjusttime기준으로 코드 작성중이므로 다른 곳에서 increatTime실행시 바뀔수 있음
        switch (cursorState){
            //현재 커서의 위치가 year에 가있는 경우
            case 0:
                year++;
                //year를 2100보다 높게 또는 2010보다 작게 설정하려고하면 year=2010로 고정 <<최대 연도는 2100년임
                if(year>2100){
                    year=2010;
                }
                break;
            case 1:
                month++;
                //month의 범위를 정상적인 범위로 한정
                //month가 12보다 커지려고 하면 1로 고정
                if(month > 12) {
                    month = 1;
                }
                break;
            case 2:
                day++;
                int limitedDay;
                limitedDay=monthMap.get(month); //현재 month에 맞는 day를 limitedday에 저장
                //user가 설정하려는 day가 제한된 day를 넘어가려고 하면 day=1;로 초기화
                if(day>limitedDay) {
                    day=1;
                }
                break;
            case 3:
                hour++;
                //설정하려는 시간이 23보다 커질경우
                if(hour>23){
                    hour=0;
                }
                break;
            case 4:
                minute++;
                if(minute>59){
                    minute=0;
                }
                break;
            case 5:
                second=0;
                break;
            default:
                break;
        }

        //increaseTime 기능을 수행후 gui세팅
        showTime();
    }

    //timer를 gui상에서 보여주자!
    private void showTimer(){
        //timer를 gui상에서 보여주기 위한 슈도코드
        //textViewHour.setText(hour);
        //textViewMinute.setTexT(minute);
        //textViewSecond.setTexT(second);
    }

    //전제조건도 다 맞춤
    //adjustsate=false일경우 진행되는 상황
    private void reqSetTimer(){
        timerRunState=timer.getRunState();

        //timer가 작동중일경우 일단 동작중인 timer를 pause시킴
        if(timerRunState==1){
            //타이머의 퍼즈타이머를 작동 시키고 runstat=0으로 만들어야함
            timer.pauseTimer();
            timerRunState=0;
            //아래꺼는 무시
            //리턴값이 주어져있음, 여기서 timerRunState의 값을 0으로 만들어야함
            //timerRunState=reqPauseTimer();
        }

        //타이머를 변경하겠다고 요청했으므로 adjustState도 true로 바꿈
        this.timerAdjustState=true;

        //메서드를 끝내고 Timer를 gui로 적용
        showTimer();
    }

    //현재 커서를 바꿔주는 기능
    private void changeCursor(){
        //현재커서위치의 값
        //curState=0=year   cursorState=1=month     cursorState=2=day
        //cursorState=3=hour    cursorState=4=minute    cursorState=5=second

        //일단 커서를 증가시키고
        cursorState++;

        //Timekeeping모드에서 커서를 증가시켰을때
        if(currentMode.equals("TimeKeeping")){
            //커서가 second에서 한단계 증가하면 year로 커서가 이동
            if(cursorState>5){
                cursorState=0;
            }
            //Timekeeing 모드동작을 수행했으므로 showTime()
            showTime();
        }
        //Timer모드에서 커서를 증가시켰을때
        else if(currentMode.equals("Timer")){
            //커서가 second에서 한단계 증가하면 hour로 커서가 이동
            if(cursorState>5){
                cursorState=3;
            }
            //Tiemr에서 동작을 수행했으므로 showTimer()
            showTimer();
        }
        //Alarm모드에서 커서 증가
        //만약 second까지 조정가능하다면 굳이 따로 할필요 없음
        //Timer와 동일하게 때문에 || currentMode.equals("Alarm") 하면됨
        else if(currentMode.equals("Alarm")){
            if(cursorState>4){
                cursorState=3;
            }
            //Alarm에서 동작을 수행했으므로 showAlarm();
            showAlarm();
        }
    }

    //setTimer단계를 끝낼때 쓰는 메서드
    private void endSetTimer(){
        //timdDB에다가 현재 설정한 timer값을 전달해줌

        //	public TimeDB() {  <<참고용
        //		setTime("2010 01 01 00 00");
        //	}
        String setTimeString = year+" "+month+" "+day+" "+hour+" "+minute;

        //timeDB에 현재 시간정보를 넘겨줌
        timeDB.setTime(setTimeString);

        //마지막으로 timer adjust를 끝냈으니 adjust단계가 종료되었다는 의미
        this.timerAdjustState=false;

        //Timer쪽 동작이 끝났으니 timer를 보여주자
        showTimer();
    }

    //adjust time 상태로 진입
    private void reqAdjustTime(){
        //timekeeping모드에서 adjust버튼을 누르면 이쪽으로

        //timeDB.getTime()메서드 실행
        //getTime으로 읽어온 값들을 저장시킴 넘어오는 값은 second까지
        String currentTime=timeDB.getTime();
        String[] tmpArray=currentTime.split(" ");

        //Stirng으로 받아온 현재 시간을 int값으로 파싱해서 저장하자
        year=Integer.parseInt(tmpArray[0]);
        month=Integer.parseInt(tmpArray[1]);
        day=Integer.parseInt(tmpArray[2]);
        minute=Integer.parseInt(tmpArray[3]);
        second=Integer.parseInt(tmpArray[4]);

        //Adjsuttime으로 들어왔다면 timekeepingAdajsutState를 true로 바꾸고
        if(!timekeepingAdjustState){
            this.timekeepingAdjustState=true;
        }

        //Timekeeping단계이므로 showTime을 실행해 gui업데이트
        showTime();
    }

    //현재 adjust페이즈일경우 그걸 종료시키는것
    private void endAdjustTime(){
        //timeDB에 현재 수정한 year, month, day, hour, minute을 전달 second는 전달해 봤자 0으로 초기화
        String currntTime=year+" "+month+" "+day+" "+hour+" "+minute;

        timeDB.setTime(currntTime);

        //그리고 adjustState=false로 만들며 phase종료
        if(timekeepingAdjustState){
            this.timekeepingAdjustState=false;
        }

        //역시 showTime으로 gui업데이트
        showTime();
    }

    //전제조건들을 만족하면 timer를 시작하는 메서드
    private void reqStartTimer(){
        //timerZeroState=true; << timer의 현재상태가 0이고
        timerZeroState=timer.getZeroState();
        //timerRunState=0; runstate도 0이어야지
        timerRunState=timer.getRunState();

        //if문을 실행
        if(timerZeroState==0 && timerRunState==0){
            //timer를 시작하라고 전달
            timer.startTimer();
        }

        //그리고 타이머가 동작중이므로 runstate를 true로 바꿈
        //timerRunState=timer.startTimer(); <<startTimer의 리턴값을 1로 줘서 sytstem의 timerRunState를 바꾸는것도 좋은 방법일듯? 이 방법은 실제 구현된건 아님
        timerRunState=1;

        //여기서 showTimer가 들어가야 될지 잘 모르겠음 startTimer를 하면 지속적으로 시간을 업데이트해줘야 하기 때문
        //showTimer();
    }

    //reset을 시켜주면 타이머의 시간을 0으로 초기화
    //timer의 시간이 00:00:00이여도 상관없이 0으로 << 이게 편함
    private void reqResetTimer(){
        //현재 타이머가 동작중인가를 확인
        timerRunState=timer.getRunState();

        //현재 타이머가 동작중이라면
        if(timerRunState==1){
            //일단 timer를 pause시킴
            timer.pauseTimer();
        }

        //그다음에 reset시키라고 메세지 보냄
        timer.resetTimer();

        //일단 리셋시켰으니 run이 아니고 zero는 맞고
        timerRunState=0;
        timerZeroState=1;

        //timer를 리셋시켰으니 그 화면을 표시
        showTimer();
    }

    private void showAlarm(){
        //gui에서 변해야 하는 값을 일단 슈도코드로 << 틀린게 있을수도 있음
        //어....알람에 loop를 하기로 안했던것같은데 << 기억이 나지 않음
        //textViewHour.setText(hour);
        //textViewminute.setTExt(minute);
    }

    //add Alarm시퀀스에서 사용되는건데 현재 코드상에서 딱히 들어갈곳이 없어보임
    //없어도 되는 메서드 일지도?
    public void showAlarmSetting(){

    }

    //다음 알람을 요청하기 위한 메서드
    private void reqNextAlarm(){
        //알람에서 가져온 알람리스트를 저장
        //alarmList=alarm.getAlarmList();/

        //알람리스트의 크기가 1보다 클경우 << 알람리스트에 알람이 존재할 경우
        if(alarmList.length>1){
            //알람에서 다음 알람을 가져와서(리턴값 존재) 내부에 표시를 해야하는데....
            //이걸 어떻게 해야할까
            //일단 showAlarm으로 맨 하단의 hour와 minute을 표시하고
            //다른 메서드를 통해 알람 상단의 다른 알람을 표시할까 << 이게 showAlarmSetting()인가?
            //alarm.getNextList();
            //ex) textViewTopAlarmHour.setText(nextAlarmTextHour); <<슈도코드
            //      textViewTopAlarmMinute.setTExT(newAlarmTextMinute);
        }

        //메서드의 마지막을 showAlarm으로 장식
        showAlarm();
    }

    //alarm화면에서  adjust버튼을 누르면 실행
    private void reqAddAlarm(){
        //만약 알람리스트에 알람이 가득 차있을경우

        //알람에서 알람리스트를 가져옴
        //alarmList=alarm.getAlarmList();

        if(alarmList.length>=4){
            //알람을 더이상 추가할수없다는 메세지 출력하기
            alarmCanAddState=false;
            alaramAdjustState=false;
        }
        //만약 알람리스트에 있는 알람이 4개가 아닐경우(알람을 더 넣을수 있을경우)
        else{
            alarmCanAddState=true;
            alaramAdjustState=true;
        }

        //여기서는 showAlarm이 필요없을것같긴한데 일단 심심하므로 추가
        showAlarm();
    }

    //알람 수정을 끝낼때 동작되는 메서드
    private void endAddAlarm(){
        //알람에 현재 수정한 알람을 집어넣음
        //alarm.addAlarm(hour, minute); <<알람에 전달해주는 인자는 임의로 선택한것

        this.alaramAdjustState=false;
        alarmCanAddState=false;

        //여기도 showAlarm이 필요 없을것 같긴한데 일단 넣음
        showAlarm();
    }

    //현재 설정된 알람을 제거한다는 메서드
    private void reqDeleteAlarm(){
        //현재 알람리스트에 설정된 알람들을 불러옴
        //alarmList=alarm.getAlarmList();

        if(alarmList.length==0){
            //알람리스트에 아무것도 없다면 에러를 출력
        }
        //알람리스트에 아무것도 없는게 아닌 뭐라도 있다면
        else{
            //현재 있는 알람을 제거하라고 시킴
            //alarm.deleteAlarm()   ;
        }

        //일단 자연스럽게 맨 마지막에 showAlarm을 삽입<< 여기도 필요없을것같긴한데?
        showAlarm();
    }

    //알람이 울릴때 stopAlarm을 누르면 알람이 정지됨
    private void reqStopAlarm(){
        //근데 이게 이미 리스너쪽에서 한번 조건문을 통과한거라 여기쓴 조건이 별로 의미가 없음 << 궁금하면 확인할것
        if(buzzByAlarm){
            //alarm.stopAlarm();
        }

        //알람을 껐으니 알람스테이트도 false로
        this.buzzByAlarm=false;

        //필요없지만 매우자연스러운 showAlarm삽입
        showAlarm();
    }

    private void showStopwatch(){
        //gui에 필요할것같은 요소들을 슈도코드로 작성
        //textViewHour.setText(hour);
        //textViewMinute.setTExT(minute);
        //textVioewSecond.setTExT(second);
        //여기 밑에부분은 아마도 stopwatch에서 가져온 데이터를 record된 데이터를 출력
        //textViewTopRecrod.setText(hour);
        //textViewTopRecord.setTExt(minute);
        //textViewTopRecored.setText(second);
    }

    //스탑워치를 시작하자
    private void reqStartStopwatch(){
        stopwatchRunState=stopwatch.getRunState();

        //이미 리스터단계에서 runState=0이 맞는지 한번 거르고 왔음
        //스탑워치가 동작중이 아닐경우 스탑워치를 실행시킴
        if(stopwatchRunState==0){
            stopwatch.startStopwatch();
        }

        //스탑워치를 실행시켰으니 런스테이트는 1로
        stopwatchRunState=1;

        //실시간으로 show를 시켜야해서 그냥 showStopwatch가 될것같진 않음
        showStopwatch();
    }

    private void reqRecordStopwatch(){
        //현재 스탑워치의 시간을 stopwatch로 보내서 저장하게 해주는 기능
    }

    public void reqPauseStopwatch(){

    }

    public void reqResestStopwatch(){

    }

    public void reqNextTide(){

    }

    public void showMoonphase(){

    }

    //현재모드 말고 다른 모드를 선택하고 싶을때 실행
    //mode select화면으로 진입하는 메서드
    public void reqModeSelect(){

    }

    //mode select화면에서 모드 셀렉트를 종료하고 빠져나올때 쓰는 메서드
    //modeslect화면을 종료할때 modeselect에 바뀐 모드들을 적용하기 위해 modeselect에 현재 선택된 모드들을 보냄
    public void endSelectMode(){
        modeSelector.setSettingModeList(selectedModes);
    }


    /*
    클래스 다이어그래에 없던 메서드들 추가
     */

    //mode select 페이즈에서 원하는 모드를 선택한 경우 그 모드가 존재하는경우 넣고 존재하지 않으면 패스
    private void addModetoList(String mode){
        //리스트의 현재 사이즈만큼 반복을 하는데
        for(int i=0;i<selectedModes.size();i++){
            //현재 선택하려고 하는 모드가 리스트안에 존재 하지 않는다면 if문 수행
            if(!selectedModes.get(i).equals(mode)){
                //리스트 안에 존재하지 않는다면 선택한 모드에 현재 선택한 모드 추가
                selectedModes.add(mode);
            }
        }
    }

    //mode select 페이즈에서 모드를 선택해제 한경우 그 모드가 존재하는 경우 삭제하고 존재하지 않으면 break;
    private void deleteModefromList(String mode){
        //리스트의 현재 사이즈만큼 반복을 하는데
        for(int i=0;i<selectedModes.size();i++){
            //현재 선택하려고하는 모드가 리스트 안에 존재한다면 if문 수행
            if(selectedModes.get(i).equals(mode)){
                //리스트 안에 존재하는 선택해제한 모드를 제거함
                selectedModes.remove(i);
                break;
            }
        }
    }

    //시퀀스에는 있는데 클래스다이어그램에는 없는건가?
    //동작중인 Tiemer를 멈추는 역할
    //이부분 조금 이상한것같은데 시퀀스 다이어그램에서 timer가 동작중이여야 퍼즈를  시작하는게 아닌감
    private void reqPauseTimer(){
        //timer로부터 현재 값이 zero인지 확인하고
        //timerZeroState=timer.getZeroState();
        //timer로부터 현재 timer가 동작중인지 확인하고
        //timerRunState=timer.getRunState();

        //timer가 0이고 동작중이지 않을경우 조건문 실행
        if(timerZeroState==0 && timerRunState==0 ){
            //timer.pauseTimer();
        }


    }

    //increaseTimer 이름을 increaseTimerTime으로 바꿈
//    public void increaseTimer(){
//
//    }

    //increseTime은 TimeKeeping과 겹치는 메서드인데 동작이 조금 다름
    //일단 새로운 increase메서드를 만듬
    private void increaseTimerTime(){
        //현재 커서에 있는 숫자를 일단 증가시키고 제어문에서 비교
        //currentNumber++;

        //현재커서위치의 값
        //curState=0=year   cursorState=1=month     cursorState=2=day
        //cursorState=3=hour    cursorState=4=minute    cursorState=5=second 단, second는 무조건 0으로 초기화
        //dayOfWeek=요일은 유저가 설정 불가능 << 알아서 계산해주는게 좋을듯
        if(cursorState==3){
            hour++;
            if(hour>99){
                hour=99;
            }
        }
        else if(cursorState==4){
            minute++;
            if(minute>59){
                minute=59;
            }
        }
        else if(cursorState==5){
            second++;
            if(second>59){
                second=59;
            }
        }
    }

    //알람의 increaseTime을 수정 하는 동작이 살짝 다름
    private void increaseAlarmTime(){
        //현재커서위치의 값
        //curState=0=year   cursorState=1=month     cursorState=2=day
        //cursorState=3=hour    cursorState=4=minute    cursorState=5=second
        //dayOfWeek=요일은 유저가 설정 불가능 << 알아서 계산해주는게 좋을듯

        if(cursorState==3){
            hour++;
            if(hour>23){
                hour=0;
            }
        }
        else if(cursorState==4){
            minute++;
            if(minute>59){
                minute=0;
            }
        }
        //second는 설정 불가능으로 하기로 했었나???? << 기억이 잘 안남
        else if(cursorState==5){
            second++;
            if(second>59){
                second=0;
            }
        }
    }

}
