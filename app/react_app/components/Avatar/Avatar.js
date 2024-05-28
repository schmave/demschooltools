import React, { useState, useEffect } from 'react';
import { Avatar as MuiAvatar } from '@mui/material';
import shuffleSeed from 'shuffle-seed';
import Typography from '../Typography/Typography';
import avatarColors from './AvatarColors';

const Avatar = (props) => {
  const [initials, setInitials] = useState('');
  const [colors, setColors] = useState({color: 'white', backgroundColor: 'black'});
  
  const { name, ...restOfProps } = props;

  useEffect(() => {
    if(name?.length > 0) {
      const initialsArray = name.split(' ');
      let newInitials = initialsArray[0].charAt(0);
      if (initialsArray.length > 1) {
        newInitials += initialsArray[initialsArray.length - 1].charAt(0);
      }
      setInitials(newInitials);
      const possibleColors = avatarColors();
      const randomColors = shuffleSeed.shuffle(possibleColors, name);
      setColors(randomColors[0]);
    }
  }, [name]);

  if(initials === '') {
    return null;
  }

  return (
    <MuiAvatar {...restOfProps} style={{ backgroundColor: colors.backgroundColor, color: colors.color }}>
      <Typography>
        {initials}
      </Typography>
    </MuiAvatar>
  );
};

export default Avatar;
