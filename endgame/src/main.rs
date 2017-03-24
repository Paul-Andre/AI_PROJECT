#[macro_use]
extern crate lazy_static;

use std::cmp::max;

use std::fs::File;
use std::io::prelude::*;
use std::io::BufWriter;

/// CONFIGURATION_COUNT[n][m] represents the numbers of ways to put m beans into n pits.
/// It is used when converting a configuration into its index.
/// It is lazily initialized using dynamic programming.
lazy_static! {
    static ref CONFIGURATION_COUNT: [[u64; 73]; 13] = {
        let mut table = [[0u64; 73]; 13];

        for i in 0..73 {
            table[0][i] = 0;
            table[1][i] = 1;
        }
        //table[0][0] = 1;

        for i in 2..13 {
            for j in 0..73 {
                let mut tot: u64 = 0;
                for k in 0..j+1 {
                    tot += table[i-1][j-k];
                }
                table[i][j] = tot;
            }
        }
        
        table
    };
}

// TODO write documentation explanation
/// CONFIGURATION_SUM[i][j]
lazy_static! {
    static ref CONFIGURATION_SUM: [[u64; 73]; 13] = {
        let mut table = [[0u64; 73]; 13];

        for i in 0..13 {
            for j in (0..72).rev() {
                table[i][j] = table[i][j+1] + CONFIGURATION_COUNT[i][j];
            }
        }

        table
    };
}
                

/// Represents a possible configuration of the game ignoring the score each player has.
#[derive(Copy,Clone,Eq,PartialEq,Debug)]
struct Configuration {
    /// The number of beans in each of the pits. Pits 0-5 are the current player's and pits are
    /// placed in such a way that rotation clockwise is equivalent to adding 1 mod 12.
    pits: [u8; 12],

    /// How much skips the current player has
    currentRemainingSkips: u8,

    /// How much skips the other player has
    otherRemainingSkips: u8
}

impl Configuration {
    /// Returns a configuration where current player and the other player are switched.
    fn switched(&self) -> Configuration {
        let mut new_pits: [u8; 12] = Default::default();
        for i in 0..6 {
            new_pits[i] = self.pits[6+i];
            new_pits[6+i] = self.pits[i];
        }
        Configuration { 
            pits: new_pits,
            currentRemainingSkips: self.otherRemainingSkips,
            otherRemainingSkips: self.currentRemainingSkips,
        }
    }

    /// Simulates a sowing step. Returns the new configuration and the amount of beans that were
    /// seized.
    fn sow(&self, n: u8) -> (Configuration, u8) {
        let mut new = self.clone();

        let mut beans = new.pits[n as usize];
        new.pits[n as usize] = 0;

        let mut i = n as usize;
        while beans != 0 {
            i = (i+1) % 12;
            new.pits[i] += 1;
            beans -= 1;
        }

        let mut seized = 0;
        while new.pits[i] == 2 || new.pits[i] == 4 || new.pits[i] == 6 {
            seized += new.pits[i];
            new.pits[i] = 0;
            i += 12-1;
            i %= 12;
        }

        (new, seized)
    }

    fn to_index(&self) -> (u8, u64) {
        let (sum, index) = partition_to_index(&self.pits);
        let mult = (self.currentRemainingSkips as u64*4 + self.otherRemainingSkips as u64);
        (sum, CONFIGURATION_COUNT[12][(sum as usize)*2]*mult + index)
    }
}

/// Returns the number of beans on the board and the index in the associated array that
/// represents this configuration.
fn partition_to_index(pits: &[u8;12]) -> (u8, u64) {
    let sum = pits.iter().sum();
    let mut remaining_sum: u8 = sum;
    let mut index: u64 = 0;
    for i in 0..12 {
        let next_remaining_sum: u8 = remaining_sum - pits[i];
        //println!("next_remaining_sum {} remaining_sum {}", next_remaining_sum, remaining_sum);
        let conrs = CONFIGURATION_SUM[12-i-1][(next_remaining_sum+1) as usize];
        let cors = CONFIGURATION_SUM[12-i-1][(remaining_sum+1) as usize];

        //println!("next_remaining_sum co {} remaining_sum co {}", conrs, cors);
        index += conrs-cors;
        remaining_sum = next_remaining_sum;
        //println!("{} {} {}", i, remaining_sum, index);
    }
    (sum/2, index)
}


#[derive(Copy,Clone,Eq,PartialEq,Debug)]
enum ConfigurationScore {
    /// The score
    Score(i8),
    /// The node wasn't visited yet
    NotVisited,
    /// The node was visited, but one of its children wasn't visited, so we placed it into a stack
    /// and postponed finding its score until the scores of its children are found.
    Postponed,
}

impl ConfigurationScore {
    fn pack(self) -> PackedConfigurationScore {
        use ConfigurationScore::*;
        PackedConfigurationScore(
            match self {
                Score(s) => s,
                NotVisited => 127,
                Postponed => 126,
            })
    }
}
            

#[derive(Copy,Clone,Eq,PartialEq)]
struct PackedConfigurationScore(i8);

impl PackedConfigurationScore {
    fn unpack(self) -> ConfigurationScore {
        use ConfigurationScore::*;
        match self {
            PackedConfigurationScore(127) => NotVisited,
            PackedConfigurationScore(126) => Postponed,
            PackedConfigurationScore(s) => Score(s),
        }
    }
}

fn assert_index_matches(c: &[u8; 12], ii: u64) {
    let (a,i) = partition_to_index(c);
    if i != ii {
        panic!("WEW");
    }
    //println!("The configuration {:?} gives the values {} = {}", c, i, ii );
}


/// Changes parition to the next partition and returns true if the last partition with that number
/// of values was obtained.
fn next_partition(array: &mut [u8]) -> bool {

    if array.len() == 1 {
        array[0] -= 1;
        true
    }
    else if array[1..].iter().sum::<u8>() == 0 {
        let len = array.len();
        array[len-1] = array[0]-1;
        array[0] = 0;
        true
    }
    else {
        if next_partition(&mut array[1..]) {
            array[0]+=1;
        }
        false
    }
}

fn get_simple_scores(scores: &mut Vec<Vec<PackedConfigurationScore>>, config: &Configuration) -> PackedConfigurationScore {
    use ConfigurationScore::*;
    let (sum, index) = config.to_index();
    if scores[sum as usize][index as usize] == NotVisited.pack() {
        if config.pits[6..12].iter().sum::<u8>() == 0 {
            scores[sum as usize][index as usize] = Score((sum*2) as i8).pack();
        }
    }
    scores[sum as usize] [index as usize]
}



fn compute_score(scores: &mut Vec<Vec<PackedConfigurationScore>>, config_: &Configuration) -> PackedConfigurationScore {
    use ConfigurationScore::*;
    if get_simple_scores(scores, config_) == NotVisited.pack() {
        let mut stack = Vec::new();
        stack.push(config_.clone());
        while let Some(config) = stack.pop() {
            //println!("{:?}", config);
            let (sum, index) = config.to_index();
            let previously_got_score = scores[sum as usize][index as usize];
            match previously_got_score.unpack() {
                NotVisited | Postponed => {
                    let switched = config.switched();
                    let mut moves = Vec::with_capacity(7);
                    if switched.currentRemainingSkips > 0 {
                        moves.push( (Configuration{
                            currentRemainingSkips: switched.currentRemainingSkips-1,
                            ..switched
                        },0) );
                    }
                    for i in 0..6u8 {
                        if (switched.pits[i as usize]!=0) {
                            moves.push(switched.sow(i));
                        }
                    }
                    let possible_scores = moves.iter().map(|&(new_config, beans_seized)| {
                        let score = get_simple_scores(scores, &new_config);
                        (new_config, match score.unpack() {
                            Score(s) => Score(s+(beans_seized as i8)).pack(),
                            something_else => something_else.pack(),
                        })
                    }).collect::<Vec<_>>();

                    /*
                            println!("Next moves are: ");
                            for &(conf,a) in &possible_scores {
                                println!(" {:?} ", (conf, a.unpack()) );
                            }
                            println!("");
                            */


                    let all_postponed = possible_scores.iter().all(|&(new_config, score)| {
                        score == Postponed.pack()
                    });
                    if all_postponed {
                        if previously_got_score == Postponed.pack() {
                            panic!("At least some of the postponed values must have been found");
                        }
                        continue;
                    }

                    let total_score = possible_scores.iter().fold(None, |acc, &(new_config, score)| {
                        match (score.unpack(),acc) {
                            (Score(a), Some(b)) => Some(max(a,b)),
                            (Score(a), None) => Some(a),
                            _ => acc
                        }
                    });

                    if (previously_got_score == Postponed.pack()) {
                        if let Some(the_score) = total_score {
                            scores[sum as usize][index as usize] = Score(-the_score).pack();
                        }
                        else {
                            scores[sum as usize][index as usize] = NotVisited.pack();
                        }
                    }
                    else {

                        let exist_not_visited = possible_scores.iter().any(|&(new_config, score)| {
                            score == NotVisited.pack()
                        });

                        if (exist_not_visited) {
                            scores[sum as usize][index as usize] = Postponed.pack();
                            stack.push(config);
                            for (new_config,score) in possible_scores {
                                if score == NotVisited.pack() {
                                    stack.push(new_config);
                                }
                            }
                        }
                        else {
                            scores[sum as usize][index as usize] = Score(-total_score.unwrap()).pack();
                        }
                    }

                },
                _ => {}
            }
        }
    }
    get_simple_scores(scores, config_)
}

fn main() {

    let mut scores: Vec<Vec<PackedConfigurationScore>> = Vec::new();
    scores.push(vec![ConfigurationScore::Score(0).pack(); 16]);

    /*
compute_score(&mut scores, &Configuration {
    pits: [0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0], currentRemainingSkips: 0, otherRemainingSkips: 0 });
    */

    /*
compute_score(&mut scores, &Configuration {
    pits: [0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1], currentRemainingSkips: 0, otherRemainingSkips: 0 });
    */


    'outer:
    for ii in 1..6 {
        let i = ii*2;

        println!("{}", i);
        println!("{}", CONFIGURATION_COUNT[12][i as usize] *16);

        scores.push(vec![ConfigurationScore::NotVisited.pack(); (CONFIGURATION_COUNT[12][i as usize]*16) as usize]);
        let mut pits = [0,0,0,0,0,0,0,0,0,0,0,i];


        loop {
            for currentRemainingSkips in 0..4 {
                for otherRemainingSkips in 0..4 {
                    let config = Configuration{
                        pits: pits,
                        currentRemainingSkips: currentRemainingSkips,
                        otherRemainingSkips: otherRemainingSkips
                    };
                    let s = compute_score(&mut scores, &config);
                    //println!("{:?}",s);
                    //println!("The config {:?} has score {:?}", config, s.unpack());
                }
            }
            if next_partition(&mut pits) {
                break;
            }
        }
    }

    println!("Will start writing to file.");

    let mut file = File::create("bulb").unwrap();
    let mut file = BufWriter::new(file);

    for (ii,a) in scores.iter().enumerate() {
        let i = ii*2;
        println!("{} {}", i, a.len() );
        for &score in a{
            
            if let ConfigurationScore::Score(the_score) = score.unpack() {
                let buffer: [u8; 1] = [the_score as u8];
                file.write_all(&buffer).unwrap();
            }
            else {
                panic!("This should all be filled out.");
            }
        }
    }

}
